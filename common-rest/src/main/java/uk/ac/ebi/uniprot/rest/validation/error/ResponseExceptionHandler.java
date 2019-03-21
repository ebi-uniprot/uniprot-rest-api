package uk.ac.ebi.uniprot.rest.validation.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.common.exception.ServiceException;
import uk.ac.ebi.uniprot.common.repository.search.QueryRetrievalException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Captures exceptions raised by the application, and handles them in a tailored way.
 *
 * @author lgonzales
 */
@ControllerAdvice
public class ResponseExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResponseExceptionHandler.class);
    private static final String NOT_FOUND_MESSAGE = "search.not.found";
    private static final String INTERNAL_ERROR_MESSAGE = "search.internal.error";

    private MessageSource messageSource;

    public ResponseExceptionHandler(MessageSource messageSource){
        this.messageSource = messageSource;
    }

    /**
     * Internal Server Error exception handler
     *
     * @param ex throw exception
     * @param request http request
     * @return 500 Internal server error response
     */
    @ExceptionHandler({QueryRetrievalException.class, ServiceException.class,Throwable.class})
    protected ResponseEntity<ErrorInfo> handleInternalServerError(Throwable ex, HttpServletRequest request) {
        logger.error("handleThrowableBadRequest: ",ex);
        List<String> messages = new ArrayList<>();
        messages.add(messageSource.getMessage(INTERNAL_ERROR_MESSAGE,null, Locale.getDefault()));
        addDebugError(request,ex,messages);

        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     * Bad Request exception handler that was catch during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler({BindException.class})
    protected ResponseEntity<ErrorInfo> handleBindExceptionBadRequest(BindException ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();

        for (FieldError error : ex.getFieldErrors()) {
            if(error.getDefaultMessage() != null) {
                messages.add(error.getDefaultMessage().replaceAll("\\{field\\}", error.getField()));
            }
        }

        for (ObjectError error : ex.getGlobalErrors()) {
            messages.add(error.getDefaultMessage());
        }

        addDebugError(request,ex,messages);

        return getBadRequestResponseEntity(request, messages);
    }

    private MediaType getContentTypeFromRequest(HttpServletRequest request) {
        MediaType result = MediaType.APPLICATION_JSON;
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if(Utils.notEmpty(acceptHeader)){
            result = MediaType.valueOf(acceptHeader);
        }
        return result;
    }

    /**
     * Bad Request exception handler that was catch during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler(value = { ConstraintViolationException.class })
    public ResponseEntity<ErrorInfo> constraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();

        for (ConstraintViolation error : ex.getConstraintViolations()) {
            if(error.getMessage() != null) {
                messages.add(error.getMessage());
            }
        }

        addDebugError(request,ex,messages);

        return getBadRequestResponseEntity(request, messages);
    }

    private ResponseEntity<ErrorInfo> getBadRequestResponseEntity(HttpServletRequest request, List<String> messages) {
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     * Resource not found exception handler that was catch during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 404 Not Found error response with error message details
     */
    @ExceptionHandler(value = {NoHandlerFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorInfo> noHandlerFoundException(Exception ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();
        messages.add(messageSource.getMessage(NOT_FOUND_MESSAGE,null, Locale.getDefault()));
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);
        addDebugError(request,ex,messages);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     *  If there is debugError in the request, this method also print exception causes to help in the debug error
     *
     * @param request Request Object.
     * @param exception the exception that was captured.
     * @param error List of existing message.
     */
    private static void addDebugError(HttpServletRequest request, Throwable exception, List<String> error) {
        if(request.getParameter("debugError") != null &&
           request.getParameter("debugError").equalsIgnoreCase("true")){

            Throwable cause = exception.getCause();
            while(cause != null) {
                if(cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                    error.add("Caused by: " + cause.getMessage());
                }
                cause = cause.getCause();
            }

        }
    }

    /**
     * Error response entity that provide error message details
     *
     * @author lgonzales
     */
    public static class ErrorInfo {
        private final String url;
        private final List<String> messages;

        ErrorInfo(String url, List<String> messages) {
            assert url != null : "Error URL cannot be null";
            assert messages != null : "Error messages cannot be null";

            this.url = url;
            this.messages = messages;
        }

        public String getUrl() {
            return url;
        }

        public List<String> getMessages() {
            return messages;
        }
    }
}
