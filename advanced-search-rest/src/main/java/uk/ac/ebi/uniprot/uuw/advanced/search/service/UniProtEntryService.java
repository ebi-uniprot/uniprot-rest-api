package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.UniProtClient;
import uk.ac.ebi.uniprot.score.UniProtEntryScored;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class UniProtEntryService {
	private UniprotQueryRepository repository;
	private UniprotFacetConfig uniprotFacetConfig;
	private UniProtClient entryService;
	private JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor;
	public UniProtEntryService(UniprotQueryRepository repository,
							   UniprotFacetConfig uniprotFacetConfig,
							   UniProtClient entryService,
							   JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor) {
		this.repository = repository;
		this.uniprotFacetConfig = uniprotFacetConfig;
		this.entryService = entryService;
		this.uniProtJsonAdaptor = uniProtJsonAdaptor;
	}
	
	public QueryResult<UPEntry> executeQuery(QuerySearchRequest searchRequest) {
		String fields = searchRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery(), uniprotFacetConfig).build();
		     simpleQuery.addProjectionOnField(new SimpleField("accession"));
			QueryResult<UniProtDocument> results = repository.searchPage(simpleQuery, searchRequest.getOffset(),
					searchRequest.getSize());
			return convertQueryDoc2UPEntry(results, filters);
		} catch (Exception e) {
			String message = "Could not get result for: [" + searchRequest + "]";
			throw new ServiceException(message, e);
		}
	}
	

	public Stream<UPEntry> getAll(String query) {
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(query, uniprotFacetConfig).build();
		     simpleQuery.addProjectionOnField(new SimpleField("accession"));
			simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
			Cursor<UniProtDocument> results = repository.getAll(simpleQuery);
			return convertStreamDoc2UPEntry(results, Collections.emptyMap());
		} catch (Exception e) {
			String message = "Could not get result for: [" + query + "]";
			throw new ServiceException(message, e);
		}
	}
	

	public QueryResult<UPEntry> executeCursorQuery(QueryCursorRequest cursorRequest) {
		String fields = cursorRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(), uniprotFacetConfig).build();
		      simpleQuery.addProjectionOnField(new SimpleField("accession"));
		  	simpleQuery.addSort(new Sort(Sort.Direction.DESC, "score"));
			simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
			QueryResult<UniProtDocument> results = repository.searchCursorPage(simpleQuery, cursorRequest.getCursor(),
					cursorRequest.getSize());
			return convertQueryDoc2UPEntry(results, filters);
		} catch (Exception e) {
			String message = "Could not get result for: [" + cursorRequest + "]";
			throw new ServiceException(message, e);
		}
	}
	
	
	private Stream<UPEntry> convertStreamDoc2UPEntry(Cursor<UniProtDocument> cursor, Map<String, List<String>> filters) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false)
				.map(doc -> convertDocToUPEntry(doc, filters))
				.flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty());

	}

	private QueryResult<UPEntry> convertQueryDoc2UPEntry(QueryResult<UniProtDocument> results, Map<String, List<String>> filters) {
		List<UPEntry> upEntries = results.getContent().stream().map(doc -> convertDocToUPEntry(doc, filters))
				.filter(val ->val.isPresent()).map(val ->val.get())
				.collect(Collectors.toList());
		return QueryResult.of(upEntries, results.getPage(), results.getFacets());

	}

	
	
	
	
	
	
	
	
	private Optional<UPEntry> convertDocToUPEntry(UniProtDocument doc,  Map<String, List<String>> filters) {
		if(doc.active) {
			Optional<UniProtEntry>  opEntry= entryService.getEntry(doc.accession);
			return opEntry.isPresent()? Optional.of(convertAndFilter(opEntry.get(), filters)): Optional.empty();
		}else {
			UPEntry upEntry = new UPEntry(doc.accession, doc.id, false, doc.inactiveReason);
			
			return Optional.of(upEntry);
		}
	}

	
	public UPEntry convertAndFilter(UniProtEntry upEntry,  Map<String, List<String>> filterParams) {
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
	

	


	public Optional<UniProtEntry> getByAccession(String accession) {
		try {
			SimpleQuery simpleQuery = new SimpleQuery(Criteria.where("accession").is(accession.toUpperCase()));
		    simpleQuery.addProjectionOnField(new SimpleField("accession"));
			Optional<UniProtDocument> result = repository.getEntry(simpleQuery);
			return result.isPresent()? entryService.getEntry(result.get().accession): Optional.empty();
		} catch (Exception e) {
			String message = "Could not get accession for: [" + accession + "]";
			throw new ServiceException(message, e);
		}
	}
	
}
