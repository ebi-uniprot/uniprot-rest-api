package org.uniprot.api.rest.output.header;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.http.HttpHeaders.*;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_RELEASE_NUMBER;

/**
 * Used by standard search/download controllers for creating headers used when searching and
 * downloading.
 *
 * <p>Created 05/12/18
 *
 * @author Edd
 */
public class HeaderFactory {
    private static final List<Pattern> PATHS_WITH_NO_CACHING =
            List.of(Pattern.compile(".*/idmapping/.*"));

    private HeaderFactory() {}

    public static HttpHeaders createHttpSearchHeader(
            HttpServletRequest request, MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);
        handleGatewayCaching(request, httpHeaders);
        return httpHeaders;
    }

    public static HttpHeaders createHttpDownloadHeader(
            MessageConverterContext context, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        MediaType mediaType = context.getContentType();
        httpHeaders.setContentType(mediaType);
        handleGatewayCaching(request, httpHeaders);
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
     * @param request the request
     * @param httpHeaders the headers to modify
     */
    private static void handleGatewayCaching(HttpServletRequest request, HttpHeaders httpHeaders) {
        boolean requiresCachingHeaders = true;
        for (Pattern pattern : PATHS_WITH_NO_CACHING) {
            if (Utils.notNull(request)
                    && Utils.notNullNotEmpty(request.getServletPath())
                    && pattern.matcher(request.getServletPath()).matches()) {
                requiresCachingHeaders = false;
                break;
            }
        }
        if (requiresCachingHeaders) {
            // used so that gate-way caching uses accept/accept-encoding headers as a key
            httpHeaders.addAll(VARY, List.of(ACCEPT, ACCEPT_ENCODING, X_RELEASE_NUMBER));
        }
    }
}
