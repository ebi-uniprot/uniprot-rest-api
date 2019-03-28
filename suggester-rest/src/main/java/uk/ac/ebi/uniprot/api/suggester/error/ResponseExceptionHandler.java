package uk.ac.ebi.uniprot.api.suggester.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary;
import uk.ac.ebi.uniprot.api.suggester.service.SuggestionRetrievalException;
import uk.ac.ebi.uniprot.api.suggester.service.UnknownDictionaryException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Captures exceptions raised by the application, and handles them in a tailored way.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@ControllerAdvice
public class ResponseExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, SuggestionRetrievalException.class})
    protected ResponseEntity<ErrorInfo> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({UnknownDictionaryException.class})
    protected ResponseEntity<ErrorInfo> handleInvalidDictionary(RuntimeException ex, HttpServletRequest request) {
        String validDictionaries = Arrays.stream(SuggestionDictionary.values()).map(SuggestionDictionary::name)
                .collect(Collectors.joining(", "));
        String message = "Invalid dictionary provided: "+ex.getMessage()+". Valid values: "+validDictionaries;
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), message);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    public static class ErrorInfo {
        private final String url;
        private final List<String> messages;

        ErrorInfo(String url, String message) {
            assert url != null : "Error URL cannot be null";
            assert message != null : "Error messages cannot be null";

            this.url = url;
            this.messages = Collections.singletonList(message);
        }

        public ErrorInfo(String url, List<String> messages) {
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
