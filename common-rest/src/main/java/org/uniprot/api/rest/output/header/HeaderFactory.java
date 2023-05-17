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
        String suffix =
                "."
                        + UniProtMediaType.getFileExtension(mediaType)
                        + context.getFileType().getExtension();
        String requestContext = getRequestContext(request);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String queryString = "";
        if (Utils.notNullNotEmpty(request.getParameter("query"))) {
            queryString = request.getParameter("query").replaceAll("[^A-Za-z0-9]", "_");
            if (queryString.length() <= 30 && !queryString.equals("_")) {
                queryString += "_" + now.format(dateTimeFormatter);
            } else if (queryString.length() > 30) {
                queryString = queryString.substring(0, 30) + "_" + now.format(dateTimeFormatter);
            } else if (queryString.equals("_")) {
                queryString = "all_" + now.format(dateTimeFormatter);
            }
        } else {
            queryString = now.format(dateTimeFormatter);
        }
        return requestContext + "_" + queryString + suffix;
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
