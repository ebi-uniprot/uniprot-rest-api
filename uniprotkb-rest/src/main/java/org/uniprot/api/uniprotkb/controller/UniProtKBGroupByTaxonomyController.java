package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.UniProtKBGroupByTaxonomyService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBGroupByTaxonomyController.GROUP_BY_TAXONOMY_RESOURCE;

@RequestMapping(value = GROUP_BY_TAXONOMY_RESOURCE)
@RestController
public class UniProtKBGroupByTaxonomyController extends UniProtKBGroupByController {
    static final String GROUP_BY_TAXONOMY_RESOURCE = GROUP_BY_RESOURCE + "/taxonomy";
    private final UniProtKBGroupByTaxonomyService uniProtKBGroupByTaxonomyService;

    @Autowired
    public UniProtKBGroupByTaxonomyController(
            UniProtKBGroupByTaxonomyService uniProtKBGroupByTaxonomyService) {
        this.uniProtKBGroupByTaxonomyService = uniProtKBGroupByTaxonomyService;
    }

    @Override
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupByResult> getGroups(
            @Parameter(
                            description =
                                    "Criteria to search the groups. It can take any valid solr query.")
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBGroupByTaxonomyService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
