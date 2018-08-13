package uk.ac.ebi.uniprot.uuw.advanced.search.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.QueryRetrievalException;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.ServiceException;

import javax.servlet.http.HttpServletRequest;
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
    private static final String NOT_FOUND_MESSAGE = "uk.ac.ebi.uniprot.uuw.advanced.search.not.found";
    private static final String INTERNAL_ERROR_MESSAGE = "uk.ac.ebi.uniprot.uuw.advanced.search.internal.error";

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

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
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

        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Resource not found exception handler that was catch during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 404 Not Found error response with error message details
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorInfo> noHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();
        messages.add(messageSource.getMessage(NOT_FOUND_MESSAGE,null, Locale.getDefault()));

        addDebugError(request,ex,messages);

        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
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
