package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.support.data.configure.response.AdvancedSearchTerm.PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.api.support.data.configure.service.TaxonomyConfigureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 11/03/2021
 */
@Tag(name = TAG_CONFIG, description = TAG_CONFIG_DESC)
@RestController
@RequestMapping("/configure/taxonomy")
public class TaxonomyConfigureController {
    private final TaxonomyConfigureService service;

    public TaxonomyConfigureController(TaxonomyConfigureService service) {
        this.service = service;
    }

    @Operation(
            summary = CONFIG_TAXONOMY_FIELDS_OPERATION,
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
                                                                            UniProtReturnField
                                                                                    .class)))
                        })
            })
    @GetMapping("/result-fields")
    public List<UniProtReturnField> getResultFields() {
        return service.getResultFields();
    }

    @Operation(
            summary = CONFIG_TAXONOMY_SEARCH_OPERATION,
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
                                                                            AdvancedSearchTerm
                                                                                    .class)))
                        })
            })
    @GetMapping("/search-fields")
    public List<AdvancedSearchTerm> getSearchFields() {
        return service.getSearchItems(PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS);
    }
}
