package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;

@Service
public class UniProtEntryService {
	private UniprotQueryRepository repository;
	private UniprotFacetConfig uniprotFacetConfig;
	private VoldemortEntryService entryService;

	public UniProtEntryService(UniprotQueryRepository repository, UniprotFacetConfig uniprotFacetConfig,
			VoldemortEntryService entryService) {
		this.repository = repository;
		this.uniprotFacetConfig = uniprotFacetConfig;
		this.entryService = entryService;
	}

	public QueryResult<UniProtEntry> executeQuery(QuerySearchRequest searchRequest) {
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery(), uniprotFacetConfig).build();
		     simpleQuery.addProjectionOnField(new SimpleField("accession"));
			QueryResult<UniProtDocument> results = repository.searchPage(simpleQuery, searchRequest.getOffset(),
					searchRequest.getSize());
			return convert(results);
		} catch (Exception e) {
			String message = "Could not get result for: [" + searchRequest + "]";
			throw new ServiceException(message, e);
		}
	}

	private QueryResult<UniProtEntry> convert(QueryResult<UniProtDocument> results) {
		List<String> accessions = results.getContent().stream().map(val -> val.accession).collect(Collectors.toList());
		Map<String, UniProtEntry> entryMap = entryService.getEntryMap(accessions);
		List<UniProtEntry> entries = accessions.stream().map(val -> entryMap.get(val)).filter((val -> val != null))
				.collect(Collectors.toList());
		return QueryResult.of(entries, results.getPage(), results.getFacets());
	}

	public QueryResult<UniProtEntry> executeCursorQuery(QueryCursorRequest cursorRequest) {
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(), uniprotFacetConfig).build();
		      simpleQuery.addProjectionOnField(new SimpleField("accession"));
		  	simpleQuery.addSort(new Sort(Sort.Direction.DESC, "score"));
			simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
			QueryResult<UniProtDocument> results = repository.searchCursorPage(simpleQuery, cursorRequest.getCursor(),
					cursorRequest.getSize());
			return convert(results);
		} catch (Exception e) {
			String message = "Could not get result for: [" + cursorRequest + "]";
			throw new ServiceException(message, e);
		}
	}

	public Stream<UniProtEntry> getAll(String query) {
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(query, uniprotFacetConfig).build();
		     simpleQuery.addProjectionOnField(new SimpleField("accession"));
			simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
			Cursor<UniProtDocument> results = repository.getAll(simpleQuery);
			return convert(results);
		} catch (Exception e) {
			String message = "Could not get result for: [" + query + "]";
			throw new ServiceException(message, e);
		}
	}

	private Stream<UniProtEntry> convert(Cursor<UniProtDocument> cursor) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false)
				.map(val->val.accession)
				.map(val -> entryService.getEntry(val))
				.flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty());

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
