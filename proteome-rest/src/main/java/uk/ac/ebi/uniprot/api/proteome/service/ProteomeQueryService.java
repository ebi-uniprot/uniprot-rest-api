package uk.ac.ebi.uniprot.api.proteome.service;

import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefSearchRequest;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeQueryRepository;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/
@Service
public class ProteomeQueryService {
    private ProteomeQueryRepository repository;
    private ProteomeFacetConfig facetConfig;

    public ProteomeQueryService(ProteomeQueryRepository repository,
    		ProteomeFacetConfig facetConfig) {
        this.repository = repository;
        this.facetConfig = facetConfig;
    }
    public QueryResult<CrossRefDocument> search(ProteomeRequest request) {
        SimpleQuery simpleQuery = createQuery(request);

        QueryResult<CrossRefDocument> results = crossRefRepository.searchPage(simpleQuery, request.getCursor(), request.getSize());

        return results;
    }

    private SimpleQuery createQuery(ProteomeRequest request) {
        SolrQueryBuilder builder = new SolrQueryBuilder();
        String requestedQuery = request.getQuery();

        builder.query(requestedQuery);
        builder.addSort(this.solrSortClause.getSort(request.getSort(), false));

        if(request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(this.crossRefFacetConfig);
        }
        return builder.build();
    }
}

