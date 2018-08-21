package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    @RequestMapping(value = "/streamAll", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> streamAll(@RequestParam(value = "query", required = true) String query) {
        AtomicInteger counter = new AtomicInteger();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        StreamingResponseBody responseBody = outputStream ->
                queryBuilderService.streamAll(query).forEach(acc -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount % 10000 == 0) {
                            outputStream.flush();
                            double rate = (double) counter.get() / stopWatch.getTotalTimeSeconds();
                            System.out.println("[total=" + currentCount + ", rate=" + rate + "]");
                        }
                        outputStream.write((acc + "\n").getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return ResponseEntity.ok().body(responseBody);
    }

    @RequestMapping(value = "/streamAll2", method = RequestMethod.GET, produces = {"text/flatfile"})
    public ResponseEntity<ResponseBodyEmitter> streamAll2(@RequestParam(value = "query", required = true) String query,
                                                          @RequestHeader("Accept") String contentType) {
        String[] contentTypeArr = contentType.split("/");
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        downloadTaskExecutor.execute(() -> {
            try {
                emitter.send(queryBuilderService.streamAll(query), new MediaType(contentTypeArr[0], contentTypeArr[1]));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
        });

        return ResponseEntity.ok().body(emitter);
    }

    @RequestMapping(value = "/searchAccession", method = RequestMethod.GET)
    public UniProtDocument getByAccession(@RequestParam(value = "accession", required = true) String accession) {
        return queryBuilderService.getByAccession(accession).orElse(null);
    }
}
