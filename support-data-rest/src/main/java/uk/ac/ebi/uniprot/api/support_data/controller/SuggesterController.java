package uk.ac.ebi.uniprot.api.support_data.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.api.suggester.Suggestions;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterService;
import uk.ac.ebi.uniprot.api.suggester.service.SuggestionRetrievalException;
import uk.ac.ebi.uniprot.api.suggester.service.UnknownDictionaryException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for the suggestion service.
 * <p>
 * Created 18/07/18
 *
 * @author Edd
 */
@RestController
public class SuggesterController {
    private final SuggesterService suggesterService;

    @Autowired
    public SuggesterController(SuggesterService suggesterService) {
        this.suggesterService = suggesterService;
    }

    @RequestMapping(value = "/suggester", produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Suggestions> suggester(
            @RequestParam(value = "dict", required = true) String dict,
            @RequestParam(value = "query", required = true) String query) {

        return new ResponseEntity<>(suggesterService.findSuggestions(dict, query),
                                    HttpStatus.OK);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Error> handleMissingParams(MissingServletRequestParameterException ex) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(
                Error.builder()
                        .message("Missing required parameter: " + ex.getParameterName())
                        .code(httpStatus.value())
                        .build(), httpStatus);
    }

    @ExceptionHandler({UnknownDictionaryException.class})
    public ResponseEntity<Error> handleMissingParams(UnknownDictionaryException ex) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(
                Error.builder()
                        .message(ex.getMessage())
                        .code(httpStatus.value())
                        .build(), httpStatus);
    }

    @ExceptionHandler({SuggestionRetrievalException.class})
    public ResponseEntity<Error> handleMissingParams(SuggestionRetrievalException ex) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(
                Error.builder()
                        .message(ex.getMessage())
                        .code(httpStatus.value())
                        .build(), httpStatus);
    }


    @Data
    @Builder
    private static class Error {
        private int code;
        private String message;
    }

}
