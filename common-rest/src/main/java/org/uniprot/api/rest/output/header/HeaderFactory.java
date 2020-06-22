package org.uniprot.api.rest.output.header;

import static org.springframework.http.HttpHeaders.*;

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

        handleGatewayCaching(httpHeaders);
        return httpHeaders;
    }

    public static HttpHeaders createHttpDownloadHeader(
            MessageConverterContext context, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        MediaType mediaType = context.getContentType();
        httpHeaders.setContentType(mediaType);
        handleGatewayCaching(httpHeaders);
        if (context.isDownloadContentDispositionHeader()) {
            String actualFileName = getContentDispositionFileName(context, request, mediaType);
            httpHeaders.setContentDispositionFormData("attachment", actualFileName);
        }
        return httpHeaders;
    }

    private static String getContentDispositionFileName(
            MessageConverterContext context, HttpServletRequest request, MediaType mediaType) {
        String suffix =
                "."
                        + UniProtMediaType.getFileExtension(mediaType)
                        + context.getFileType().getExtension();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss.SS");
        String queryString;
        if (Utils.notNullNotEmpty(request.getQueryString())) {
            queryString = request.getQueryString().replaceAll("[^A-Za-z0-9]", "_");
            if (queryString.length() > 60) {
                queryString = queryString.substring(0, 60);
            }
            queryString += "-" + now.format(dateTimeFormatter);
        } else {
            queryString = now.format(dateTimeFormatter);
        }
        return "uniprot-" + queryString + suffix;
    }

    /**
     * Ensure gate-way caching uses accept/accept-encoding headers as a key
     *
     * @param httpHeaders the headers to modify
     */
    private static void handleGatewayCaching(HttpHeaders httpHeaders) {
        // used so that gate-way caching uses accept/accept-encoding headers as a key
        httpHeaders.add(VARY, ACCEPT);
        httpHeaders.add(VARY, ACCEPT_ENCODING);
    }
}
