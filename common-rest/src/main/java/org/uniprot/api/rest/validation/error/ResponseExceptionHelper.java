package org.uniprot.api.rest.validation.error;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.owasp.encoder.Encode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.uniprot.core.util.Utils;

/**
 * Created 20/04/2020
 *
 * @author Edd
 */
public class ResponseExceptionHelper {

    private ResponseExceptionHelper(){

    }
    /**
     * If there is debugError in the request, this method also print exception causes to help in the
     * debug error
     *
     * @param request Request Object.
     * @param exception the exception that was captured.
     * @param error List of existing message.
     */
    public static void addDebugError(
            HttpServletRequest request, Throwable exception, List<String> error) {
        if (request.getParameter("debugError") != null
                && request.getParameter("debugError").equalsIgnoreCase("true")) {

            error.add("Message: " + exception.getMessage());
            Arrays.stream(exception.getStackTrace())
                    .sequential()
                    .forEach(element -> error.add("StackTrace: " + element.toString()));
            Throwable cause = exception.getCause();
            while (cause != null) {
                if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                    error.add("Caused by: " + cause.getMessage());
                }
                if (cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
                    Arrays.stream(cause.getStackTrace())
                            .sequential()
                            .limit(5)
                            .forEach(
                                    element ->
                                            error.add(
                                                    "Caused by StackTrace: " + element.toString()));
                }
                cause = cause.getCause();
            }
        }
    }

    public static MediaType getContentTypeFromRequest(HttpServletRequest request) {
        MediaType result = MediaType.APPLICATION_JSON;
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if (Utils.notNullNotEmpty(acceptHeader)) {
            result = MediaType.valueOf(acceptHeader);
        }
        return result;
    }

    public static ResponseEntity<ErrorInfo> getBadRequestResponseEntity(
            HttpServletRequest request, List<String> messages) {
        String safeURL = Encode.forHtml(request.getRequestURL().toString());
        ErrorInfo error = new ErrorInfo(safeURL, messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }
}
