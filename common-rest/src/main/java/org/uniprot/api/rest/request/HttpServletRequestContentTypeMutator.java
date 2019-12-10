package org.uniprot.api.rest.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private HttpServletRequestContentTypeMutator() {}

    public static void mutate(MutableHttpServletRequest request) {
        handleEntryRequest(request);
        handleSearchOrDownloadRequest(request);
    }

    private static void handleSearchOrDownloadRequest(MutableHttpServletRequest request) {
        String format = request.getParameter(FORMAT);
        if ((request.getRequestURI().endsWith(DOWNLOAD) || request.getRequestURI().endsWith(SEARCH))
                && Objects.nonNull(format)) {
            addContentTypeHeaderForFormat(request, format);
        }
    }

    private static void addContentTypeHeaderForFormat(
            MutableHttpServletRequest request, String format) {
        request.addHeader(
                HttpHeaders.ACCEPT,
                UniProtMediaType.getMediaTypeForFileExtension(format).toString());
    }

    private static void handleEntryRequest(MutableHttpServletRequest request) {
        Matcher entryContextMatcher = ENTRY_CONTEXT_PATH_MATCHER.matcher(request.getRequestURL());
        if (entryContextMatcher.matches()) {
            String entryPathVariable = entryContextMatcher.group(2);
            String entryId = entryContextMatcher.group(3);
            String extension = entryContextMatcher.group(4);

            setRealEntryId(request, entryPathVariable, entryId);

            setURI(request, extension);
            setURL(request, extension);

            addContentTypeHeaderForFormat(request, extension);
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
