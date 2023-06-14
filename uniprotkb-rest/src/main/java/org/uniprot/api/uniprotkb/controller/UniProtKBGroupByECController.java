package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.uniprotkb.controller.UniProtKBGroupByECController.GROUP_BY_EC_RESOURCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.UniProtKBGroupByECService;

import io.swagger.v3.oas.annotations.Parameter;

@RequestMapping(value = GROUP_BY_EC_RESOURCE)
@RestController
public class UniProtKBGroupByECController extends UniProtKBGroupByController {
    static final String GROUP_BY_EC_RESOURCE = GROUP_BY_RESOURCE + "/ec";
    private final UniProtKBGroupByECService uniProtKBGroupByECService;

    @Autowired
    public UniProtKBGroupByECController(UniProtKBGroupByECService uniProtKBGroupByECService) {
        this.uniProtKBGroupByECService = uniProtKBGroupByECService;
    }

    @Override
    @GetMapping
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
                uniProtKBGroupByECService.getGroups(query, parent), HttpStatus.OK);
    }
}
