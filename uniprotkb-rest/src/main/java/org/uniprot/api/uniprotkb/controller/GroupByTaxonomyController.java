package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.uniprotkb.controller.GroupByTaxonomyController.GROUP_BY_TAXONOMY_RESOURCE;

import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByTaxonomyService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RequestMapping(value = GROUP_BY_TAXONOMY_RESOURCE)
@RestController
@Validated
public class GroupByTaxonomyController extends GroupByController {
    static final String GROUP_BY_TAXONOMY_RESOURCE = GROUPS + "/taxonomy";
    private final GroupByTaxonomyService uniProtKBGroupByTaxonomyService;

    @Autowired
    public GroupByTaxonomyController(GroupByTaxonomyService uniProtKBGroupByTaxonomyService) {
        this.uniProtKBGroupByTaxonomyService = uniProtKBGroupByTaxonomyService;
    }

    @Operation(
            hidden = true,
            summary = GROUP_TAXONOMY_OPERATION,
            description = GROUP_TAXONOMY_OPERATION_DESC)
    @ApiResponse(
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GroupByResult.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupByResult> getGroups(
            @Parameter(description = QUERY_UNIPROTKB_TAXONOMY_DESCRIPTION)
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = GROUP_PARENT_DESCRIPTION)
                    @Pattern(
                            regexp = FieldRegexConstants.TAXONOMY_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.taxonomy.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBGroupByTaxonomyService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
