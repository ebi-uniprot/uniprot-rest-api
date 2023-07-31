package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.GroupByECService;

import javax.validation.constraints.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.GroupByECController.GROUP_BY_EC_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

@RequestMapping(value = GROUP_BY_EC_RESOURCE)
@RestController
@Validated
public class GroupByECController extends GroupByController {
    static final String GROUP_BY_EC_RESOURCE = (UNIPROTKB_RESOURCE + GROUPS) + "/ec";
    private static final String EC_ID_REGEX =
            "^([1-7])\\.("
                    + "((\\d{1,2})\\.(\\d{1,2})\\.(\\d{1,3}|n\\d{1,2}|-))|"
                    + "((\\d{1,2})\\.(\\d{1,2}|-)\\.-)|"
                    + "((\\d{1,2}|-)\\.-\\.-)"
                    + ")$";
    private final GroupByECService uniProtKBGroupByECService;

    @Autowired
    public GroupByECController(GroupByECService uniProtKBGroupByECService) {
        this.uniProtKBGroupByECService = uniProtKBGroupByECService;
    }

    @Tag(name = "uniprotkbgroup")
    @Operation(summary = "List of groups w.r.t. to the given query and parent")
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
                            regexp = EC_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message =
                                    "The parent ec id value should be in "
                                            + EC_ID_REGEX
                                            + " format")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBGroupByECService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
