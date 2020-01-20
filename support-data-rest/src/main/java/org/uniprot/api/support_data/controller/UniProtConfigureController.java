package org.uniprot.api.support_data.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.UniProtConfigureService;
import org.uniprot.core.cv.xdb.UniProtXDbTypeDetail;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.domain.SearchItem;

@RestController
@RequestMapping("/configure/uniprotkb")
public class UniProtConfigureController {
    private UniProtConfigureService service;

    public UniProtConfigureController(UniProtConfigureService service) {
        this.service = service;
    }

    @GetMapping("/search_terms")
    public List<SearchItem> getUniProtSearchTerms() {
        return service.getUniProtSearchItems();
    }

    @GetMapping("/search-terms")
    public List<org.uniprot.store.search.domain2.SearchItem> getUniProtSearchTerms2() {
        return service.getUniProtSearchItems2();
    }

    @GetMapping("/annotation_evidences")
    public List<EvidenceGroup> getUniProtAnnotationEvidences() {
        return service.getAnnotationEvidences();
    }

    @GetMapping("/go_evidences")
    public List<EvidenceGroup> getUniProtGoEvidences() {
        return service.getGoEvidences();
    }

    @GetMapping("/databases")
    public List<DatabaseGroup> getUniProtDatabase() {
        return service.getDatabases();
    }

    @GetMapping("/resultfields")
    public List<FieldGroup> getResultFields() {
        return service.getResultFields();
    }

    @GetMapping("/allDatabases")
    public List<UniProtXDbTypeDetail> getUniProtAllDatabase() {
        return service.getAllDatabases();
    }
}
