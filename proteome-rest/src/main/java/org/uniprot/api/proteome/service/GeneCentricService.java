package org.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.proteome.request.GeneCentricRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.field.GeneCentricField.Search;

import java.util.stream.Stream;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */
@Service
public class GeneCentricService {
	private final GeneCentricSortClause solrSortClause;
	private GeneCentricFacetConfig facetConfig;
	private final BasicSearchService<CanonicalProtein, GeneCentricDocument> basicService;
	private final DefaultSearchHandler defaultSearchHandler;

	@Autowired
	public GeneCentricService(GeneCentricQueryRepository repository, GeneCentricFacetConfig facetConfig,
			GeneCentricSortClause solrSortClause) {
		basicService = new BasicSearchService<>(repository, new GeneCentricEntryConverter());
		this.facetConfig = facetConfig;
		this.solrSortClause = solrSortClause;
		this.defaultSearchHandler = new DefaultSearchHandler(Search.accession, Search.accession_id,
				Search.getBoostFields());
	}

	public QueryResult<CanonicalProtein> search(GeneCentricRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.search(query, request.getCursor(), request.getSize());
	}

	public CanonicalProtein getByAccession(String accession) {
		return basicService.getEntity(Search.accession_id.name(), accession.toUpperCase());
	}

	public Stream<CanonicalProtein> download(GeneCentricRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.download(query);
	}
}
