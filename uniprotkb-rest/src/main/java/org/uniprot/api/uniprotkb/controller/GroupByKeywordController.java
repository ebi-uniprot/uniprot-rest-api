package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.GroupByKeywordController.GROUP_BY_KEYWORD_RESOURCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.GroupByKeywordService;

import io.swagger.v3.oas.annotations.Parameter;

@RequestMapping(value = GROUP_BY_KEYWORD_RESOURCE)
@RestController
public class GroupByKeywordController extends GroupByController {
    static final String GROUP_BY_KEYWORD_RESOURCE = GROUP_BY_RESOURCE + "/keyword";
    private final GroupByKeywordService uniProtKBGroupByKeywordService;

    @Autowired
    public GroupByKeywordController(GroupByKeywordService uniProtKBGroupByKeywordService) {
        this.uniProtKBGroupByKeywordService = uniProtKBGroupByKeywordService;
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
                uniProtKBGroupByKeywordService.getGroupByResult(query, parent), HttpStatus.OK);
    }
}
