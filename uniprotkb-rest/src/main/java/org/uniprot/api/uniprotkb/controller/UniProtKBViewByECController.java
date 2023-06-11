package org.uniprot.api.uniprotkb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.uniprotkb.view.ViewByResult;
import org.uniprot.api.uniprotkb.view.service.UniProtKBViewByECService;

import static org.uniprot.api.uniprotkb.controller.UniProtKBViewByECController.VIEW_BY_EC_RESOURCE;

@RequestMapping(value = VIEW_BY_EC_RESOURCE)
@RestController
public class UniProtKBViewByECController extends UniProtKBViewByController {
    static final String VIEW_BY_EC_RESOURCE = VIEW_BY_RESOURCE + "/ec";
    private final UniProtKBViewByECService uniProtKBViewByECService;

    @Autowired
    public UniProtKBViewByECController(UniProtKBViewByECService uniProtKBViewByECService) {
        this.uniProtKBViewByECService = uniProtKBViewByECService;
    }

    @Override
    @GetMapping
    public ResponseEntity<ViewByResult> getViewBys(String query, String parent) {
        return new ResponseEntity<>(uniProtKBViewByECService.getViewBys(query, parent), HttpStatus.OK);
    }
}
