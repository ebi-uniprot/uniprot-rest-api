package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

public abstract class GroupByController {
    static final String GROUP_BY_RESOURCE = UNIPROTKB_RESOURCE + "/groups";

    @Tag(name = "uniprotkbgroup")
    @Operation(summary = "List of groups w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(
                            mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GroupByResult.class)))
    public abstract ResponseEntity<GroupByResult> getGroups(String query, String parent);
}
