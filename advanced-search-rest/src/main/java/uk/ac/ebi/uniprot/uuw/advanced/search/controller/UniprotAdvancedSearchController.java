package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.score.UniProtEntryScored;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private final UniProtEntryService entryService;
	private JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor;

    @Autowired
    public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
                                           UniprotAdvancedSearchService queryBuilderService,
                                           ThreadPoolTaskExecutor downloadTaskExecutor,
                                           UniProtEntryService entryService,
										   JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor) {
        this.eventPublisher = eventPublisher;
        this.queryBuilderService = queryBuilderService;
        this.downloadTaskExecutor = downloadTaskExecutor;
        this.entryService = entryService;
        this.uniProtJsonAdaptor = uniProtJsonAdaptor;
    }


	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ResponseEntity<QueryResult<UPEntry>> search(@Valid QuerySearchRequest searchRequest,
			HttpServletRequest request, HttpServletResponse response) {

		QueryResult<UniProtEntry> queryResult = entryService.executeQuery(searchRequest);
		String fields = searchRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		QueryResult<UPEntry> result = convert(queryResult, filters);

		eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPage()));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(value = "/searchCursor", method = RequestMethod.GET)
	public ResponseEntity<QueryResult<UPEntry>> searchCursor(@Valid QueryCursorRequest cursorRequest,
			HttpServletRequest request, HttpServletResponse response) {

		QueryResult<UniProtEntry> queryResult = entryService.executeCursorQuery(cursorRequest);
		String fields = cursorRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		QueryResult<UPEntry> result = convert(queryResult, filters);
		eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPage()));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}


	@RequestMapping(value = "/searchAll", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Stream<UPEntry>> searchAll(@RequestParam(value = "query", required = true) String query,
			@RequestParam(value = "field", required = false) String field) {
		Stream<UniProtEntry> stream = entryService.getAll(query);
		Map<String, List<String>> filters = FieldsParser.parse(field);
		Stream<UPEntry> entryStream = stream.map(val -> uniProtJsonAdaptor.convertEntity(val, filters));
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
		if (doc == null)
			return null;
		Optional<UniProtEntry> result = entryService.getByAccession(doc.accession);
		if (result.isPresent()) {
			return convertAndFilter(result.get(), Collections.emptyMap());
		} else

			return null;
	}

	private QueryResult<UPEntry> convert(QueryResult<UniProtEntry> results, Map<String, List<String>> filters) {
		List<UPEntry> upEntries = results.getContent().stream().map(val -> convertAndFilter(val, filters))
				.collect(Collectors.toList());
		return QueryResult.of(upEntries, results.getPage(), results.getFacets());

	}

	private UPEntry convertAndFilter(UniProtEntry upEntry,  Map<String, List<String>> filterParams) {
		UPEntry entry  = uniProtJsonAdaptor.convertEntity(upEntry, filterParams);
		if((filterParams ==null ) || filterParams.isEmpty())
			return entry;
		EntryFilters.filterEntry(entry, filterParams);
		if(filterParams.containsKey("score")) {
			entry.setAnnotationScore(getScore(upEntry));
		}
		return entry;
	}
	private int getScore(UniProtEntry entry) {
		 UniProtEntryScored entryScored = new UniProtEntryScored(entry);
		 double score = entryScored.score();
		 int q = (int) (score / 20d);
		 int normalisedScore= q > 4 ? 5 : q + 1;
		 return normalisedScore;
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
