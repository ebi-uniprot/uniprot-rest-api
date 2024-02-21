package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.TAG_CONFIG;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.TAG_CONFIG_DESC;

import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.support.data.configure.response.SolrJsonQuery;
import org.uniprot.api.support.data.configure.service.UtilService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = TAG_CONFIG, description = TAG_CONFIG_DESC)
@RestController
@RequestMapping("/util")
@Validated
public class UtilController {
    private final UtilService service;

    public UtilController(UtilService service) {
        this.service = service;
    }

    @Operation(
            hidden = true,
            summary = CONFIG_UTIL_QUERY_PARSER_OPERATION,
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
                                                                            SolrJsonQuery.class)))
                        })
            })
    @GetMapping("/queryParser")
    public SolrJsonQuery parseSolrQuery(
            @Parameter(
                            description = CONFIG_UTIL_QUERY_DESCRIPTION,
                            example = CONFIG_UTIL_QUERY_EXAMPLE)
                    @NotNull(message = "{query.parameter.required}")
                    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
                    @RequestParam(name = "query")
                    String query) {
        return service.convertQuery(query);
    }
}
