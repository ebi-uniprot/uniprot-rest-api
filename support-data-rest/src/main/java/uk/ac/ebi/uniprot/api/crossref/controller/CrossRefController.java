package uk.ac.ebi.uniprot.api.crossref.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRef;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefSearchRequest;
import uk.ac.ebi.uniprot.api.crossref.service.CrossRefService;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/v1/xref")
@Validated
public class CrossRefController {
    @Autowired
    private CrossRefService crossRefService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @GetMapping(value = "/accession/{accessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrossRef getByAccession(@PathVariable("accessionId") String accession){
        CrossRef crossRef = crossRefService.findByAccession(accession);
        return crossRef;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public QueryResult<CrossRef> searchCursor(@Valid CrossRefSearchRequest searchRequest,
                                              HttpServletRequest request, HttpServletResponse response) {

        QueryResult<CrossRef> result = crossRefService.search(searchRequest);

        this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));

        return result;
    }
}