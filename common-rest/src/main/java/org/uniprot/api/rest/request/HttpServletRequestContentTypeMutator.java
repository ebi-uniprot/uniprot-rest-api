package org.uniprot.api.rest.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created 03/12/2019
 *
 * @author Edd
 */
@Slf4j
public class HttpServletRequestContentTypeMutator {
    private static final String FORMAT = "format";
    private static final String SEARCH = "/search";
    private static final String DOWNLOAD = "/download";
    private static final Pattern ENTRY_CONTEXT_PATH_MATCHER = Pattern.compile("^.*/(.*)\\.(\\w+)$");

    public static MutableHttpServletRequest handle(
            MutableHttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleEntryRequest(request, response);
        handleSearchOrDownloadRequest(request);

        return request;
    }

    private static void handleSearchOrDownloadRequest(MutableHttpServletRequest request) {
        if (request.getRequestURI().endsWith(DOWNLOAD)
                || request.getRequestURI().endsWith(SEARCH)) {

            String format = request.getParameter(FORMAT);
            if (Utils.notNullOrEmpty(format)) {
                switch (format) {
                    case "json":
                        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        break;
                    case "xml":
                        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
                        break;
                    case "fasta":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
                        break;
                    case "obo":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.OBO_MEDIA_TYPE_VALUE);
                        break;
                    case "rdf":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE_VALUE);
                        break;
                    case "gff":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE_VALUE);
                        break;
                    case "xls":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);
                        break;
                    case "tsv":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);
                        break;
                    case "txt":
                        request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.FF_MEDIA_TYPE_VALUE);
                        break;
                    case "list":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
                        break;
                    default:
                        log.warn("Unknown format parameter received: " + format);
                }
            }
        }
    }

    private static void handleEntryRequest(
            MutableHttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Matcher entryContextMatcher = ENTRY_CONTEXT_PATH_MATCHER.matcher(request.getRequestURI());
        if (entryContextMatcher.matches()) {
            String uriNoExtension = entryContextMatcher.group(1);
            //            request.setRequestURI(uriNoExtension);

            String extension = entryContextMatcher.group(2);

            if (!extension.isEmpty()) {
                switch (extension) {
                    case "json":
                        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        break;
                    case "xml":
                        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
                        break;
                    case "fasta":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
                        break;
                    case "obo":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.OBO_MEDIA_TYPE_VALUE);
                        break;
                    case "rdf":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE_VALUE);
                        break;
                    case "gff":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE_VALUE);
                        break;
                    case "xls":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);
                        break;
                    case "tsv":
                        request.addHeader(
                                HttpHeaders.ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);
                        break;
                    case "txt":
                        request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.FF_MEDIA_TYPE_VALUE);
                        break;
                    default:
                        log.warn("Unknown extension requested: " + extension);
                }

//                request.getRequestDispatcher(uriNoExtension).forward(request, response);
                request.getRequestDispatcher("/accession/P12345").forward(request, response);
            }
        }
    }
}
