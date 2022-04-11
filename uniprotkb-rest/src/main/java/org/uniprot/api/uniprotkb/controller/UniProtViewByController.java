package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.service.*;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Hidden
@RestController
@RequestMapping("/uniprotkb/view")
public class UniProtViewByController {

    private final UniProtViewByECService viewByECService;
    private final UniProtViewByKeywordService viewByKeywordService;
    private final UniProtViewByPathwayService viewByPathwayService;
    private final UniProtViewByGoService viewByGoService;
    private final UniProtViewByTaxonomyService viewByTaxonomyService;

    @Autowired
    public UniProtViewByController(
            UniProtViewByECService viewByECService,
            UniProtViewByKeywordService viewByKeywordService,
            UniProtViewByPathwayService viewByPathwayService,
            UniProtViewByGoService viewByGoService,
            UniProtViewByTaxonomyService viewByTaxonomyService) {
        this.viewByECService = viewByECService;
        this.viewByKeywordService = viewByKeywordService;
        this.viewByPathwayService = viewByPathwayService;
        this.viewByGoService = viewByGoService;
        this.viewByTaxonomyService = viewByTaxonomyService;
    }

    @Hidden
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
        List<ViewBy> viewBys = viewByECService.get(query, parent);
        return new ResponseEntity<>(viewBys, HttpStatus.OK);
    }

    @Hidden
    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/keyword",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<List<ViewBy>> getKeyword(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByKeywordService.get(query, parent), HttpStatus.OK);
    }

    @Hidden
    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/pathway",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<List<ViewBy>> getPathway(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByPathwayService.get(query, parent), HttpStatus.OK);
    }

    @Hidden
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
        return new ResponseEntity<>(viewByGoService.get(query, parent), HttpStatus.OK);
    }

    @Hidden
    @Tag(name = "uniprotkbview")
    @GetMapping(
            value = "/taxonomy",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")
    @ApiResponse(
            content =
                    @Content(array = @ArraySchema(schema = @Schema(implementation = ViewBy.class))))
    public ResponseEntity<List<ViewBy>> getTaxonomy(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query", required = true)
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(viewByTaxonomyService.get(query, parent), HttpStatus.OK);
    }
}
