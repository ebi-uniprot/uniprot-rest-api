package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.GroupByGOController.GROUP_BY_GO_RESOURCE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.GO_ID_REGEX;

import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.GroupByGOService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RequestMapping(value = GROUP_BY_GO_RESOURCE)
@RestController
@Validated
public class GroupByGOController extends GroupByController {
    static final String GROUP_BY_GO_RESOURCE = GROUPS + "/go";
    private final GroupByGOService uniProtKBGroupByGoService;

    @Autowired
    public GroupByGOController(GroupByGOService uniProtKBGroupByGoService) {
        this.uniProtKBGroupByGoService = uniProtKBGroupByGoService;
    }

    @Operation(summary = "List of groups with respect to the given query and parent")
    @ApiResponse(
            content =
                    @Content(
                            mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GroupByResult.class)))
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupByResult> getGroups(
            @Parameter(
                            description =
                                    "Criteria to search the groups. It can take any valid solr query.")
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = "Name of the parent")
                    @Pattern(
                            regexp = GO_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.go.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBGroupByGoService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
