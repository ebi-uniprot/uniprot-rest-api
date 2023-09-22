package org.uniprot.api.rest.output.header;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;

/**
 * Used by standard search/download controllers for creating headers used when searching and
 * downloading.
 *
 * <p>Created 05/12/18
 *
 * @author Edd
 */
public class HeaderFactory {

    private static final String QUERY_STRING_MULTIPLE_UNDERSCORE_REGEX = "_{2,10}";
    private static final String UNDERSCORE = "_";

    private HeaderFactory() {}

    public static HttpHeaders createHttpSearchHeader(MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);
        return httpHeaders;
    }

    public static HttpHeaders createHttpDownloadHeader(
            MessageConverterContext context, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        MediaType mediaType = context.getContentType();
        httpHeaders.setContentType(mediaType);
        if (context.isDownloadContentDispositionHeader()) {
            String actualFileName = getContentDispositionFileName(context, request, mediaType);
            httpHeaders.setContentDispositionFormData("attachment", actualFileName);
        }
        return httpHeaders;
    }

    private static String getContentDispositionFileName(
            MessageConverterContext context, HttpServletRequest request, MediaType mediaType) {
        String fileName = "";
        String queryString = "";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String suffix =
                UNDERSCORE
                        + now.format(dateTimeFormatter)
                        + "."
                        + UniProtMediaType.getFileExtension(mediaType)
                        + context.getFileType().getExtension();
        String requestContext = getRequestContext(request);
        if (Utils.notNullNotEmpty(request.getParameter("query"))) {
            queryString =
                    request.getParameter("query")
                            .replaceAll("[^A-Za-z0-9]", UNDERSCORE)
                            .replaceAll(QUERY_STRING_MULTIPLE_UNDERSCORE_REGEX, UNDERSCORE);
            if (queryString.length() > 30) {
                queryString = queryString.substring(0, 30);
            } else if (queryString.equals(UNDERSCORE)) {
                queryString = "all";
            }
        }
        fileName = requestContext + UNDERSCORE + queryString + suffix;
        fileName = fileName.replaceAll(QUERY_STRING_MULTIPLE_UNDERSCORE_REGEX, UNDERSCORE);
        return fileName;
    }

    private static String getRequestContext(HttpServletRequest request) {
        String requestContext = "";
        String requestURI = request.getRequestURI();
        if (requestURI != null) {
            requestContext = requestURI.split("/")[1];
        }
        return requestContext;
    }
}
