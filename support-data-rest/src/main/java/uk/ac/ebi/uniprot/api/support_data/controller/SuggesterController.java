package uk.ac.ebi.uniprot.api.support_data.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.uniprot.api.suggester.Suggestions;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.ac.ebi.uniprot.api.suggester.service.SuggestionValidator.getSuggestionDictionary;

/**
 * Controller for the suggestion service.
 *
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
        return new ResponseEntity<>(
                suggesterService.getSuggestions(getSuggestionDictionary(dict), query),
                HttpStatus.OK);
    }
}
