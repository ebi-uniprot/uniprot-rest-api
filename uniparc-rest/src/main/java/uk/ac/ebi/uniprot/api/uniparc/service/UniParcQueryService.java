package uk.ac.ebi.uniprot.api.uniparc.service;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.api.uniparc.repository.UniParcFacetConfig;
import uk.ac.ebi.uniprot.api.uniparc.repository.UniParcQueryRepository;
import uk.ac.ebi.uniprot.api.uniparc.request.UniParcRequest;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument;
import uk.ac.ebi.uniprot.search.field.UniParcField.Search;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/
@Service
public class UniParcQueryService {
	private final UniParcSortClause solrSortClause;
	private UniParcFacetConfig facetConfig;
	private final BasicSearchService<UniParcEntry, UniParcDocument> basicService;
	private final DefaultSearchHandler defaultSearchHandler;

	@Autowired
	public UniParcQueryService(UniParcQueryRepository repository, UniParcFacetConfig facetConfig,
			UniParcSortClause solrSortClause) {
		basicService = new BasicSearchService<>(repository, new UniParcEntryConverter());
		this.facetConfig = facetConfig;
		this.solrSortClause = solrSortClause;
		this.defaultSearchHandler = new DefaultSearchHandler(Search.content, Search.upi,
				Search.getBoostFields());
	}

	public QueryResult<UniParcEntry> search(UniParcRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.search(query, request.getCursor(), request.getSize());
	}

	public UniParcEntry getById(String upi) {
		return basicService.getEntity(Search.upi.name(), upi.toUpperCase());
	}

	public Stream<UniParcEntry> download(UniParcRequest request) {
		SolrRequest query = basicService.createSolrRequest(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.download(query);
	}
}

