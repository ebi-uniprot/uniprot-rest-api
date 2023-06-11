package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.uniprotkb.view.ViewByResult;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

public abstract class UniProtKBViewByController {
    static final String VIEW_BY_RESOURCE = UNIPROTKB_RESOURCE + "/view";

    @Tag(name = "uniprotkbview")
    @Operation(summary = "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ViewByResult.class)))
    public abstract ResponseEntity<ViewByResult> getViewBys(String query, String parent);

   /* @Tag(
            name = "uniprotkbview",
            description =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @GetMapping(
            value = "/ec",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(summary = "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<ViewByResult> getViewByEC(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByECService.getViewBys(query, parent), HttpStatus.OK);
    }

    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/keyword",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(summary = "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<ViewByResult> getViewByKeyword(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByKeywordService.getViewBys(query, parent), HttpStatus.OK);
    }

    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/go",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<ViewByResult> getGo(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByGoService.getViewBys(query, parent), HttpStatus.OK);
    }

    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/taxonomy",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(summary = "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<ViewByResult> getViewByTaxonomy(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(uniProtKBViewByTaxonomyService.getViewBys(query, parent), HttpStatus.OK);
    }*/
}
