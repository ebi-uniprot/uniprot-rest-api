package org.uniprot.api.support_data.controller;

import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.support_data.configure.service.UtilService;
import org.uniprot.api.support_data.configure.uniprot.domain.query.SolrJsonQuery;

@RestController
@RequestMapping("/util")
@Validated
public class UtilController {
    private UtilService service;

    public UtilController(UtilService service) {
        this.service = service;
    }

    @GetMapping("/queryParser")
    public SolrJsonQuery parseSolrQuery(
            @NotNull(message = "{query.parameter.required}")
                    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
                    String query) {
        return service.convertQuery(query);
    }
}
