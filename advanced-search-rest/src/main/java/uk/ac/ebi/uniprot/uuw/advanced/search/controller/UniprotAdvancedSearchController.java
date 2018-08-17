package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@RequestMapping("/uniprot")
public class UniprotAdvancedSearchController {

    private final ApplicationEventPublisher eventPublisher;

    private final UniprotAdvancedSearchService queryBuilderService;

    @Autowired
    public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
                                           UniprotAdvancedSearchService queryBuilderService) {
        this.eventPublisher = eventPublisher;
        this.queryBuilderService = queryBuilderService;
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<QueryResult<UniProtDocument>> searchPage(@Valid QuerySearchRequest searchRequest,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {

        QueryResult<UniProtDocument> queryResult =  queryBuilderService.executeQuery(searchRequest);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this,request,response,queryResult.getPage()));
        return new ResponseEntity<>(queryResult, HttpStatus.OK);
    }


    @RequestMapping(value = "/searchCursor", method = RequestMethod.GET)
    public ResponseEntity<QueryResult<UniProtDocument>> searchCursor(@Valid QueryCursorRequest cursorRequest,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {

        QueryResult<UniProtDocument> queryResult =  queryBuilderService.executeCursorQuery(cursorRequest);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this,request,response,queryResult.getPage()));
        return new ResponseEntity<>(queryResult, HttpStatus.OK);
    }


    @RequestMapping(value = "/searchAll", method = RequestMethod.GET)
    public ResponseEntity<Stream<UniProtDocument>> searchAll(@RequestParam(value = "query", required = true) String query) {
        Cursor<UniProtDocument> cursor =  queryBuilderService.getAll(query);

        Stream<UniProtDocument> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @RequestMapping(value = "/streamAll", method = RequestMethod.GET)
    public ResponseEntity<Stream<String>> streamAll(@RequestParam(value = "query", required = true) String query) {
        return new ResponseEntity<>(queryBuilderService.streamAll(query), HttpStatus.OK);
    }


    @RequestMapping(value = "/searchAccession", method = RequestMethod.GET)
    public UniProtDocument getByAccession(@RequestParam(value = "accession", required = true) String accession) {
        return queryBuilderService.getByAccession(accession).orElse(null);
    }

}
