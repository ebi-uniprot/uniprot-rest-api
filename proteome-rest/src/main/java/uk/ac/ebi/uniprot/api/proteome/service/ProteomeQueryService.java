package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.stream.Stream;

import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeRequest;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;
import uk.ac.ebi.uniprot.search.field.ProteomeField.Search;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
 */
@Service
public class ProteomeQueryService {
	private ProteomeFacetConfig facetConfig;
	private final ProteomeSortClause solrSortClause;
	private final DefaultSearchHandler defaultSearchHandler;
	private final BasicSearchService<ProteomeEntry, ProteomeDocument> basicService;

	public ProteomeQueryService(ProteomeQueryRepository repository, ProteomeFacetConfig facetConfig,
			ProteomeSortClause solrSortClause) {
		this.facetConfig = facetConfig;
		this.solrSortClause = solrSortClause;
		this.defaultSearchHandler = new DefaultSearchHandler(Search.content, Search.upid, Search.getBoostFields());
		basicService = new BasicSearchService<>(repository, new ProteomeEntryConverter());
	}

	public QueryResult<ProteomeEntry> search(ProteomeRequest request) {
		SimpleQuery query = basicService.createSolrQuery(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.search(query, request.getCursor(), request.getSize());

	}

	public ProteomeEntry getByUPId(String upid) {
		return basicService.getEntity(Search.upid.name(), upid.toUpperCase());
	}

	public Stream<ProteomeEntry> download(ProteomeRequest request) {
		SimpleQuery query = basicService.createSolrQuery(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.download(query);
	}

}
