package org.uniprot.api.uniref.service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.api.uniref.request.UniRefRequest;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.uniref.UniRefDocument;
import org.uniprot.store.search.field.UniRefField;
import org.uniprot.store.search.field.UniRefField.Search;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
 */
@Service
public class UniRefQueryService {
	private final UniRefSortClause solrSortClause;
	private UniRefFacetConfig facetConfig;
	private final BasicSearchService<UniRefEntry, UniRefDocument> basicService;
	private final DefaultSearchHandler defaultSearchHandler;

	private final StoreStreamer<UniRefEntry> storeStreamer;

	private final UniRefQueryRepository repository;

	private final UniRefQueryResultConverter resultsConverter;

	@Autowired
	public UniRefQueryService(UniRefQueryRepository repository, UniRefFacetConfig facetConfig,
			UniRefSortClause solrSortClause, UniRefStoreClient entryStore, StoreStreamer<UniRefEntry> storeStreamer) {
		basicService = new BasicSearchService<>(repository, new UniRefEntryConverter(entryStore));
		this.facetConfig = facetConfig;
		this.solrSortClause = solrSortClause;
		this.defaultSearchHandler = new DefaultSearchHandler(Search.content, Search.upi, Search.getBoostFields());
		this.storeStreamer = storeStreamer;
		this.repository = repository;
		this.resultsConverter = new UniRefQueryResultConverter(entryStore);
	}

	public QueryResult<UniRefEntry> search(UniRefRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);

		QueryResult<UniRefDocument> results = repository.searchPage(query, request.getCursor(), request.getSize());

		return resultsConverter.convertQueryResult(results, Collections.emptyMap());
	}

	public UniRefEntry getById(String id) {
		try {

			SolrRequest solrRequest = SolrRequest.builder().query(UniRefField.Search.id.name() + ":" + id).build();
			Optional<UniRefDocument> optionalDoc = repository.getEntry(solrRequest);
			Optional<UniRefEntry> optionalRefEntry = optionalDoc
					.map(doc -> resultsConverter.convertDoc(doc, Collections.emptyMap()))
					.orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

			return optionalRefEntry.orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			String message = "Could not get uniref id for: [" + id + "]";
			throw new ServiceException(message, e);
		}

	}

	public Stream<UniRefEntry> stream(UniRefRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);
		return storeStreamer.idsToStoreStream(query);

	}

	public Stream<String> streamIds(UniRefRequest request) {
		SolrRequest solrRequest = basicService.createSolrRequest(request, facetConfig, solrSortClause,
				defaultSearchHandler);
		return storeStreamer.idsStream(solrRequest);
	}

}
