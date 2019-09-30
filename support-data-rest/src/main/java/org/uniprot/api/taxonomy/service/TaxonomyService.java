package org.uniprot.api.taxonomy.service;

import static org.uniprot.store.search.field.TaxonomyField.Search;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.taxonomy.repository.TaxonomyRepository;
import org.uniprot.api.taxonomy.request.TaxonomyRequestDTO;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

@Service
public class TaxonomyService {

    private final BasicSearchService<TaxonomyEntry, TaxonomyDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final TaxonomyFacetConfig facetConfig;
    private final TaxonomySortClause taxonomySortClause;

    public TaxonomyService(TaxonomyRepository repository, TaxonomyFacetConfig facetConfig) {
        this.basicService = new BasicSearchService<>(repository, new TaxonomyEntryConverter());
        this.facetConfig = facetConfig;
        this.defaultSearchHandler =
                new DefaultSearchHandler(Search.content, Search.id, Search.getBoostFields());
        this.taxonomySortClause = new TaxonomySortClause();
    }

    public TaxonomyEntry findById(final long taxId) {
        return basicService.getEntity(Search.id.name(), String.valueOf(taxId));
    }

    public QueryResult<TaxonomyEntry> search(TaxonomyRequestDTO request) {
        SolrRequest solrQuery =
                basicService.createSolrRequest(
                        request, facetConfig, taxonomySortClause, defaultSearchHandler);
        return basicService.search(solrQuery, request.getCursor(), request.getSize());
    }

    public Stream<TaxonomyEntry> download(TaxonomyRequestDTO request) {
        SolrRequest query =
                basicService.createSolrRequest(
                        request, facetConfig, taxonomySortClause, defaultSearchHandler);
        return basicService.download(query);
    }
}
