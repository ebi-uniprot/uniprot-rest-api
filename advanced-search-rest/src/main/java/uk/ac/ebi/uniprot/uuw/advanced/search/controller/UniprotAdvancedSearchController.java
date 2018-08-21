package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService;

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
	private final UniProtEntryService entryService;
	@Inject
	private JsonDataAdapter<UniProtEntry, UPEntry> jsonEntryAdaptor;

	@Autowired
	public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
			UniprotAdvancedSearchService queryBuilderService, UniProtEntryService entryService) {
		this.eventPublisher = eventPublisher;
		this.queryBuilderService = queryBuilderService;
		this.entryService = entryService;
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
		Stream<UPEntry> entryStream = stream.map(val -> jsonEntryAdaptor.convertEntity(val, filters));
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
			return jsonEntryAdaptor.convertEntity(result.get(), Collections.emptyMap());
		} else

			return null;
	}

	QueryResult<UPEntry> convert(QueryResult<UniProtEntry> results, Map<String, List<String>> filters) {
		List<UPEntry> upEntries = results.getContent().stream().map(val -> jsonEntryAdaptor.convertEntity(val, filters))
				.collect(Collectors.toList());
		return QueryResult.of(upEntries, results.getPage(), results.getFacets());

	}

}
