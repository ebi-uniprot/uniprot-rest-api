package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.VARY;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory.Resource.UNIPROT;

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
    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory converterContextFactory;

    @Autowired
    public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
                                           UniprotAdvancedSearchService queryBuilderService,
                                           ThreadPoolTaskExecutor downloadTaskExecutor,
                                           UniProtEntryService entryService,
                                           MessageConverterContextFactory converterContextFactory) {
        this.eventPublisher = eventPublisher;
        this.queryBuilderService = queryBuilderService;
        this.downloadTaskExecutor = downloadTaskExecutor;
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }


	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ResponseEntity<QueryResult<UPEntry>> search(@Valid QuerySearchRequest searchRequest,
			HttpServletRequest request, HttpServletResponse response) {

		QueryResult<UPEntry> queryResult = entryService.executeQuery(searchRequest);

		eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, queryResult.getPageAndClean()));
		return new ResponseEntity<>(queryResult, HttpStatus.OK);
	}

	@RequestMapping(value = "/searchCursor", method = RequestMethod.GET)
	public ResponseEntity<QueryResult<UPEntry>> searchCursor(@Valid QueryCursorRequest cursorRequest,
			HttpServletRequest request, HttpServletResponse response) {
		QueryResult<UPEntry> result = entryService.executeCursorQuery(cursorRequest);
		eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}


	@RequestMapping(value = "/searchAll", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Stream<UPEntry>> searchAll(@RequestParam(value = "query", required = true) String query,
			@RequestParam(value = "field", required = false) String field) {
		Stream<UPEntry> entryStream = entryService.getAll(query);
		return new ResponseEntity<>(entryStream, HttpStatus.OK);
	}

	@RequestMapping(value = "/searchAccession", method = RequestMethod.GET)
	public UniProtDocument getBySearchAccession(@RequestParam(value = "accession", required = true) String accession) {
		UniProtDocument doc = queryBuilderService.getByAccession(accession).orElse(null);

		return doc;
	}

	@RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE )
	public UPEntry getByAccession(@PathVariable String accession) {
		UniProtDocument doc = queryBuilderService.getByAccession(accession).orElse(null);
		if((doc == null) || !doc.active)
			return null;
		Optional<UniProtEntry> result = entryService.getByAccession(doc.accession);
		if (result.isPresent()) {
			return entryService.convertAndFilter(result.get(), Collections.emptyMap());
		} else

			return null;
	}


    /*
     * E.g., usage from command line:
     *
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/advancedsearch/uniprot/stream?query=reviewed:true" (for just accessions)
     * time curl -OJ -H "Accept:text/flatfile" "http://localhost:8090/advancedsearch/uniprot/stream?query=reviewed:true" (for entries)
     * time curl -OJ -H "Accept:application/xml" "http://localhost:8090/advancedsearch/uniprot/stream?query=reviewed:true" (for XML entries)
     *
     * for GZIPPED results, add -H "Accept-Encoding:gzip"
     *
     * Note that by setting content-disposition header, we a file is downloaded (and it's not written to stdout).
     */
    @RequestMapping(value = "/stream", method = RequestMethod.GET,
            produces = {"text/flatfile", "text/list", "application/xml"})
    public ResponseEntity<ResponseBodyEmitter> stream2(@RequestParam(value = "query", required = true) String query,
                                                      @RequestHeader("Accept") MediaType contentType,
                                                      @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
                                                      HttpServletRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        MessageConverterContext context = converterContextFactory.get(UNIPROT, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));

        queryBuilderService.stream(query, context, emitter);

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(contentType, context, request))
                .body(emitter);
    }

    private HttpHeaders createHttpDownloadHeader(MediaType mediaType, MessageConverterContext context, HttpServletRequest request) {
        String fileName = "uniprot-" + request.getQueryString() + "." + mediaType
                .getSubtype() + context.getFileType().getExtension();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", fileName);
        httpHeaders.setContentType(mediaType);

        // used so that gate-way caching uses accept/accept-encoding headers as a key
        httpHeaders.add(VARY, ACCEPT);
        httpHeaders.add(VARY, ACCEPT_ENCODING);
        return httpHeaders;
    }
}
