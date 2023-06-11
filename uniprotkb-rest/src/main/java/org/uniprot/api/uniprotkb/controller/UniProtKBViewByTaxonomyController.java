package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.view.ViewByResult;
import org.uniprot.api.uniprotkb.view.service.UniProtKBViewByTaxonomyService;

import static org.uniprot.api.uniprotkb.controller.UniProtKBViewByTaxonomyController.VIEW_BY_TAXONOMY_RESOURCE;

@RequestMapping(value = VIEW_BY_TAXONOMY_RESOURCE)
@RestController
public class UniProtKBViewByTaxonomyController extends UniProtKBViewByController {
    static final String VIEW_BY_TAXONOMY_RESOURCE = VIEW_BY_RESOURCE + "/taxonomy";
    private final UniProtKBViewByTaxonomyService uniProtKBViewByTaxonomyService;

    @Autowired
    public UniProtKBViewByTaxonomyController(UniProtKBViewByTaxonomyService uniProtKBViewByTaxonomyService) {
        this.uniProtKBViewByTaxonomyService = uniProtKBViewByTaxonomyService;
    }

    @Override
    @GetMapping
    public ResponseEntity<ViewByResult> getViewBys(
            @Parameter(description = "Criteria to search the views. It can take any valid solr query.")
            @RequestParam(value = "query") String query,
            @Parameter(description = "Name of the parent")
            @RequestParam(value = "parent", required = false) String parent) {
        return new ResponseEntity<>(uniProtKBViewByTaxonomyService.getViewBys(query, parent), HttpStatus.OK);
    }
}
