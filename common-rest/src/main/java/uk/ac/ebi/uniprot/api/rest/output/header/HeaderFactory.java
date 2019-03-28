package uk.ac.ebi.uniprot.api.rest.output.header;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.VARY;

/**
 * Used by standard search/download controllers for creating headers used when searching and
 * downloading.
 * <p>
 * Created 05/12/18
 *
 * @author Edd
 */
public class HeaderFactory {
    public static HttpHeaders createHttpSearchHeader(MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(mediaType);

        handleGatewayCaching(httpHeaders);
        return httpHeaders;
    }

    public static HttpHeaders createHttpDownloadHeader(MessageConverterContext context, HttpServletRequest request) {
        MediaType mediaType = context.getContentType();
        String suffix = "." + UniProtMediaType.getFileExtension(mediaType) + context.getFileType().getExtension();
        String queryString = request.getQueryString();
        String desiredFileName = "uniprot-" + queryString + suffix;
        String actualFileName;

        // truncate the file name if the query makes it too long -- instead use date + truncated query
        if (desiredFileName.length() > 200) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd@HH:mm:ss.SS");
            String timestamp = now.format(dateTimeFormatter);
            int queryStrLength = queryString.length();
            int queryStringTruncatePoint = queryStrLength > 50 ? 50 : queryStrLength;
            actualFileName = "uniprot-" + timestamp + "-" + queryString.substring(0, queryStringTruncatePoint) + suffix;
        } else {
            actualFileName = desiredFileName;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", actualFileName);
        httpHeaders.setContentType(mediaType);
        handleGatewayCaching(httpHeaders);

        return httpHeaders;
    }

    /**
     * Ensure gate-way caching uses accept/accept-encoding headers as a key
     * @param httpHeaders the headers to modify
     */
    private static void handleGatewayCaching(HttpHeaders httpHeaders) {
        // used so that gate-way caching uses accept/accept-encoding headers as a key
        httpHeaders.add(VARY, ACCEPT);
        httpHeaders.add(VARY, ACCEPT_ENCODING);
    }
}
