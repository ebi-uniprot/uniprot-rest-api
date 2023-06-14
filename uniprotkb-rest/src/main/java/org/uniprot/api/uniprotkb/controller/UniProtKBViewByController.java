package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import org.springframework.http.ResponseEntity;
import org.uniprot.api.uniprotkb.view.ViewByResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

public abstract class UniProtKBViewByController {
    static final String VIEW_BY_RESOURCE = UNIPROTKB_RESOURCE + "/view";

    @Tag(name = "uniprotkbview")
    @Operation(summary = "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(
                            mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ViewByResult.class)))
    public abstract ResponseEntity<ViewByResult> getViewBys(String query, String parent);
}
