package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.VARY;

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
    private final ThreadPoolTaskExecutor downloadTaskExecutor;

    @Autowired
    public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
                                           UniprotAdvancedSearchService queryBuilderService,
                                           ThreadPoolTaskExecutor downloadTaskExecutor) {
        this.eventPublisher = eventPublisher;
        this.queryBuilderService = queryBuilderService;
        this.downloadTaskExecutor = downloadTaskExecutor;
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<QueryResult<UniProtDocument>> searchPage(@Valid QuerySearchRequest searchRequest,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {

        QueryResult<UniProtDocument> queryResult = queryBuilderService.executeQuery(searchRequest);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, queryResult.getPage()));
        return new ResponseEntity<>(queryResult, HttpStatus.OK);
    }


    @RequestMapping(value = "/searchCursor", method = RequestMethod.GET)
    public ResponseEntity<QueryResult<UniProtDocument>> searchCursor(@Valid QueryCursorRequest cursorRequest,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {

        QueryResult<UniProtDocument> queryResult = queryBuilderService.executeCursorQuery(cursorRequest);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, queryResult.getPage()));
        return new ResponseEntity<>(queryResult, HttpStatus.OK);
    }


    @RequestMapping(value = "/searchAll", method = RequestMethod.GET)
    public ResponseEntity<Stream<UniProtDocument>> searchAll(@RequestParam(value = "query", required = true) String query) {
        Cursor<UniProtDocument> cursor = queryBuilderService.getAll(query);

        Stream<UniProtDocument> stream = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @RequestMapping(value = "/searchAccession", method = RequestMethod.GET)
    public UniProtDocument getByAccession(@RequestParam(value = "accession", required = true) String accession) {
        return queryBuilderService.getByAccession(accession).orElse(null);
    }

    /*
     * E.g., usage from command line:
     *
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/advancedsearch/uniprot/stream?query=reviewed:true" (for just accessions)
     * time curl -OJ -H "Accept:text/flatfile" "http://localhost:8090/advancedsearch/uniprot/stream?query=reviewed:true" (for entries)
     *
     * Note that by setting content-disposition header, we a file is downloaded (and it's not written to stdout).
     */
    @RequestMapping(value = "/stream", method = RequestMethod.GET, produces = {"text/flatfile", "text/list"})
    public ResponseEntity<ResponseBodyEmitter> stream(@RequestParam(value = "query", required = true) String query,
                                                      @RequestHeader("Accept") MediaType contentType,
                                                      HttpServletRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        downloadTaskExecutor.execute(() -> {
            try {
                emitter.send(queryBuilderService.stream(query, contentType), contentType);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
        });

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(contentType, request))
                .body(emitter);
    }

    private HttpHeaders createHttpDownloadHeader(MediaType mediaType, HttpServletRequest request) {
        String queryString = request.getQueryString();
        String fileName = "UniProt-Download-" + queryString + "." + mediaType.getSubtype();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", fileName);
        httpHeaders.setContentType(mediaType);
        httpHeaders.add(VARY, ACCEPT); // used so that gate-way caching uses accept header as a key
        return httpHeaders;
    }
}
