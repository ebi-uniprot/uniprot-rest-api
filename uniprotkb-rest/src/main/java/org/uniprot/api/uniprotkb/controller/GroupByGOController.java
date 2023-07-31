package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.GroupByGOController.GROUP_BY_GO_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

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
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping(value = GROUP_BY_GO_RESOURCE)
@RestController
@Validated
public class GroupByGOController extends GroupByController {
    static final String GROUP_BY_GO_RESOURCE = (UNIPROTKB_RESOURCE + GROUPS) + "/go";
    private static final String GO_ID_REGEX = "^GO:\\d{7}$";
    private final GroupByGOService uniProtKBGroupByGoService;

    @Autowired
    public GroupByGOController(GroupByGOService uniProtKBGroupByGoService) {
        this.uniProtKBGroupByGoService = uniProtKBGroupByGoService;
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
                            regexp = GO_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message =
                                    "The parent go id value should be in "
                                            + GO_ID_REGEX
                                            + " format")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBGroupByGoService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
