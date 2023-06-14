package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.uniprotkb.controller.UniProtKBViewByKeywordController.VIEW_BY_KEYWORD_RESOURCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.view.ViewByResult;
import org.uniprot.api.uniprotkb.view.service.UniProtKBViewByKeywordService;

import io.swagger.v3.oas.annotations.Parameter;

@RequestMapping(value = VIEW_BY_KEYWORD_RESOURCE)
@RestController
public class UniProtKBViewByKeywordController extends UniProtKBViewByController {
    static final String VIEW_BY_KEYWORD_RESOURCE = VIEW_BY_RESOURCE + "/keyword";
    private final UniProtKBViewByKeywordService uniProtKBViewByKeywordService;

    @Autowired
    public UniProtKBViewByKeywordController(
            UniProtKBViewByKeywordService uniProtKBViewByKeywordService) {
        this.uniProtKBViewByKeywordService = uniProtKBViewByKeywordService;
    }

    @Override
    @GetMapping
    public ResponseEntity<ViewByResult> getViewBys(
            @Parameter(
                            description =
                                    "Criteria to search the views. It can take any valid solr query.")
                    @RequestParam(value = "query")
                    String query,
            @Parameter(description = "Name of the parent")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return new ResponseEntity<>(
                uniProtKBViewByKeywordService.getViewBys(query, parent), HttpStatus.OK);
    }
}
