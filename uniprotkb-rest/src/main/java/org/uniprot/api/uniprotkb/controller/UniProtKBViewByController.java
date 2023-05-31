package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByResult;
import org.uniprot.api.uniprotkb.view.service.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBViewByController.*;

@RestController
@RequestMapping(VIEW_BY_RESOURCE)
public class UniProtKBViewByController {
    static final String VIEW_BY_RESOURCE = UNIPROTKB_RESOURCE + "/view";
    private final UniProtViewByECService viewByECService;
    private final UniProtViewByKeywordService viewByKeywordService;
    private final UniProtViewByGoService viewByGoService;
    private final UniProtKBViewByTaxonomyService uniProtKBViewByTaxonomyService;

    @Autowired
    public UniProtKBViewByController(
            UniProtViewByECService viewByECService,
            UniProtViewByKeywordService viewByKeywordService,
            UniProtViewByGoService viewByGoService,
            UniProtKBViewByTaxonomyService uniProtKBViewByTaxonomyService) {
        this.viewByECService = viewByECService;
        this.viewByKeywordService = viewByKeywordService;
        this.viewByGoService = viewByGoService;
        this.uniProtKBViewByTaxonomyService = uniProtKBViewByTaxonomyService;
    }

    @Tag(
            name = "uniprotkbview",
            description =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @GetMapping(
            value = "/ec",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<List<ViewBy>> getEC(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        List<ViewBy> viewBys = viewByECService.getViewBys(query, parent);
        return new ResponseEntity<>(viewBys, HttpStatus.OK);
    }

    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/keyword",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<List<ViewBy>> getViewByKeyword(
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
    public ResponseEntity<List<ViewBy>> getGo(
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
    @Operation(
            summary =
                    "List of view-bys w.r.t. to the given query and parent")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<ViewByResult<ViewBy>> getViewByTaxonomy(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(new ViewByResult<>(uniProtKBViewByTaxonomyService.getViewBys(query, parent)),
                HttpStatus.OK);
    }
}
