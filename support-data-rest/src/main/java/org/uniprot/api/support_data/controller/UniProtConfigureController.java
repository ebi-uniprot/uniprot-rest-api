package org.uniprot.api.support_data.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.UniProtConfigureService;
import org.uniprot.api.configure.uniprot.domain.model.AdvanceUniProtKBSearchTerm;
import org.uniprot.api.configure.uniprot.domain.model.UniProtReturnField;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.FieldGroup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/configure/uniprotkb")
public class UniProtConfigureController {
    private UniProtConfigureService service;
    private String searchTermResponse;

    public UniProtConfigureController(UniProtConfigureService service) {
        this.service = service;
    }

    // FIXME Delete this method once UI team starts consuming response of getUniProtSearchTerms
    @GetMapping("/search_terms")
    public ResponseEntity<String> getUniProtSearchTermsTemp()
            throws IOException, URISyntaxException {
        if (searchTermResponse == null) {
            URI uri =
                    UniProtConfigureController.class
                            .getResource("/search_terms-response.json")
                            .toURI();
            searchTermResponse = new String(Files.readAllBytes(Paths.get(uri)));
        }

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new ResponseEntity<>(searchTermResponse, httpHeaders, HttpStatus.OK);
    }

    @GetMapping("/search-terms")
    public List<AdvanceUniProtKBSearchTerm> getUniProtSearchTerms() {
        return service.getUniProtSearchItems();
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

    @GetMapping("/result-fields")
    public List<UniProtReturnField> getResultFields2() {
        return service.getResultFields2();
    }

    @GetMapping("/allDatabases")
    public List<UniProtDatabaseDetail> getUniProtAllDatabase() {
        return service.getAllDatabases();
    }
}
