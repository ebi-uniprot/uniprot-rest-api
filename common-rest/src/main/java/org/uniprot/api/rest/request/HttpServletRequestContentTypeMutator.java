package org.uniprot.api.rest.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
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

    public static MutableHttpServletRequest handle(MutableHttpServletRequest request) {
        handleEntryRequest(request);
        handleSearchOrDownloadRequest(request);

        return request;
    }

    private static void handleSearchOrDownloadRequest(MutableHttpServletRequest request) {
        if (request.getRequestURI().endsWith(DOWNLOAD)
                || request.getRequestURI().endsWith(SEARCH)) {
            String format = request.getParameter(FORMAT);
            addContentTypeHeaderForFormat(request, format);
        }
    }

    private static void addContentTypeHeaderForFormat(
            MutableHttpServletRequest request, String format) {
        if (Utils.notNullOrEmpty(format)) {
            switch (format) {
                case "json":
                    request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    break;
                case "xml":
                    request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
                    break;
                case "fasta":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
                    break;
                case "obo":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.OBO_MEDIA_TYPE_VALUE);
                    break;
                case "rdf":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE_VALUE);
                    break;
                case "gff":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE_VALUE);
                    break;
                case "xls":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);
                    break;
                case "tsv":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);
                    break;
                case "txt":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.FF_MEDIA_TYPE_VALUE);
                    break;
                case "list":
                    request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
                    break;
                default:
                    log.warn("Unknown format: " + format);
            }
        }
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
