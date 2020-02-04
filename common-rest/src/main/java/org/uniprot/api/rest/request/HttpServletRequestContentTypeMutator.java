package org.uniprot.api.rest.request;

import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.createUnknownMediaTypeForFileExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.HandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

/**
 * A helper class that mutates an {@link HttpServletRequest} based on its values, and if necessary
 * sets the request's content type in the 'Accept' header.
 *
 * <p>Created 03/12/2019
 *
 * @author Edd
 */
@Slf4j
public class HttpServletRequestContentTypeMutator {
    private static final String FORMAT = "format";
    private static final String SEARCH = "/search";
    private static final String DOWNLOAD = "/download";
    private static final Pattern ENTRY_CONTEXT_PATH_MATCHER =
            Pattern.compile("^(.*/(.*)/(.*))\\.(\\w+)$");
    private static final Set<String> ALLOWED_ACCEPT_HEADERS =
            UniProtMediaType.ALL_TYPES.stream().map(MimeType::toString).collect(Collectors.toSet());

    private HttpServletRequestContentTypeMutator() {}

    public static void mutate(MutableHttpServletRequest request) {
        boolean mutated = mutateEntryRequest(request);
        mutated = mutated || mutateSearchOrDownloadRequest(request);
        addDefaultAcceptHeaderIfRequired(mutated, request);
    }

    private static boolean mutateSearchOrDownloadRequest(MutableHttpServletRequest request) {
        boolean mutated = false;
        String format = request.getParameter(FORMAT);
        if ((request.getRequestURI().endsWith(DOWNLOAD) || request.getRequestURI().endsWith(SEARCH))
                && Utils.notNullNotEmpty(format)) {
            addContentTypeHeaderForFormat(request, format);
            mutated = true;
        }
        return mutated;
    }

    private static boolean mutateEntryRequest(MutableHttpServletRequest request) {
        boolean mutated = false;
        Matcher entryContextMatcher = ENTRY_CONTEXT_PATH_MATCHER.matcher(request.getRequestURL());
        if (entryContextMatcher.matches()) {
            String entryPathVariable = entryContextMatcher.group(2);
            String entryId = entryContextMatcher.group(3);
            String extension = entryContextMatcher.group(4);

            setRealEntryId(request, entryPathVariable, entryId);

            setURI(request, extension);
            setURL(request, extension);

            addContentTypeHeaderForFormat(request, extension);
            mutated = true;
        }
        return mutated;
    }

    private static void addDefaultAcceptHeaderIfRequired(
            boolean mutated, MutableHttpServletRequest request) {
        // if no accept header was added based on format/extension, then add default content type
        if (!mutated
                && (Utils.nullOrEmpty(request.getHeader(HttpHeaders.ACCEPT))
                        || (Utils.notNullNotEmpty(request.getHeader(HttpHeaders.ACCEPT))
                                && (request.getHeader(HttpHeaders.ACCEPT).equals("*/*")
                                        || !ALLOWED_ACCEPT_HEADERS.contains(
                                                request.getHeader(HttpHeaders.ACCEPT)))))) {
            request.addHeader(HttpHeaders.ACCEPT, DEFAULT_MEDIA_TYPE_VALUE);
        }
    }

    private static void addContentTypeHeaderForFormat(
            MutableHttpServletRequest request, String format) {
        try {
            MediaType mediaTypeForFileExtension =
                    UniProtMediaType.getMediaTypeForFileExtension(format);
            request.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());
        } catch (IllegalArgumentException iae) {
            request.addHeader(
                    HttpHeaders.ACCEPT, createUnknownMediaTypeForFileExtension(format).toString());
        }
    }

    private static void setURL(MutableHttpServletRequest request, String extension) {
        request.setRequestURL(
                request.getRequestURL()
                        .substring(0, request.getRequestURL().length() - (extension.length() + 1)));
    }

    private static void setURI(MutableHttpServletRequest request, String extension) {
        request.setRequestURI(
                request.getRequestURI()
                        .substring(0, request.getRequestURI().length() - (extension.length() + 1)));
    }

    private static void setRealEntryId(
            MutableHttpServletRequest request, String entryPathVariable, String entryId) {
        Map<String, String> uriVariablesMap = new HashMap<>();
        uriVariablesMap.put(entryPathVariable, entryId);

        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariablesMap);
    }
}
