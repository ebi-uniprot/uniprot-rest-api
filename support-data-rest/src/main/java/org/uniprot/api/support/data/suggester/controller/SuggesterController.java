package org.uniprot.api.support.data.suggester.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.suggester.response.Suggestions;
import org.uniprot.api.support.data.suggester.service.SuggesterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for the suggestion service.
 *
 * <p>Created 18/07/18
 *
 * @author Edd
 */
@Tag(
        name = "Suggester",
        description =
                "This service provides configuration data used in UniProt website for suggester (auto-complete) data")
@RestController
public class SuggesterController {
    private final SuggesterService suggesterService;

    @Autowired
    public SuggesterController(SuggesterService suggesterService) {
        this.suggesterService = suggesterService;
    }

    @Operation(
            summary =
                    "Provide suggestions (auto-complete) for a subset of datasets (dictionaries).",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            Suggestions.class)))
                        })
            })
    @GetMapping(
            value = "/suggester",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Suggestions> suggester(
            @Parameter(
                            description =
                                    "Suggest dictionary. The list of valid dictionaries is: KEYWORD, SUBCELL, MAIN, TAXONOMY, GO, EC, CATALYTIC_ACTIVITY, ORGANISM, HOST or CHEBI")
                    @RequestParam(value = "dict", required = true)
                    String dict,
            @Parameter(description = "Text to look up for auto-complete. For example: huma")
                    @RequestParam(value = "query", required = true)
                    String query) {

        return new ResponseEntity<>(suggesterService.findSuggestions(dict, query), HttpStatus.OK);
    }
}
