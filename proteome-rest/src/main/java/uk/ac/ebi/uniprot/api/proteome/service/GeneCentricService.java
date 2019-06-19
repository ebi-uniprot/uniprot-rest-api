package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricRequest;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument;
import uk.ac.ebi.uniprot.search.field.GeneCentricField.Search;

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
		SimpleQuery query = basicService.createSolrQuery(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.search(query, request.getCursor(), request.getSize());
	}

	public CanonicalProtein getByAccession(String accession) {
		return basicService.getEntity(Search.accession_id.name(), accession.toUpperCase());
	}

	public Stream<CanonicalProtein> download(GeneCentricRequest request) {
		SimpleQuery query = basicService.createSolrQuery(request, facetConfig, solrSortClause, defaultSearchHandler);
		return basicService.download(query);
	}
}
