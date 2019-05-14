package uk.ac.ebi.uniprot.api.taxonomy.service;

import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.service.SearchRetrievalService;
import uk.ac.ebi.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import uk.ac.ebi.uniprot.api.taxonomy.repository.TaxonomyRepository;
import uk.ac.ebi.uniprot.api.taxonomy.request.TaxonomyRequestDTO;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;

import static uk.ac.ebi.uniprot.search.field.TaxonomyField.Search;

@Service
public class TaxonomyService {

    private final SearchRetrievalService<TaxonomyEntry,TaxonomyDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final TaxonomyFacetConfig facetConfig;
    private final TaxonomySortClause taxonomySortClause;

    public TaxonomyService(TaxonomyRepository repository, TaxonomyFacetConfig facetConfig){
        this.basicService = new SearchRetrievalService<>(repository, new TaxonomyEntryConverter());
        this.facetConfig = facetConfig;
        this.defaultSearchHandler = new DefaultSearchHandler(Search.content,Search.id,Search.getBoostFields());
        this.taxonomySortClause = new TaxonomySortClause();
    }

    public TaxonomyEntry findById(final long taxId) {
        return basicService.getEntity(Search.id.name(), String.valueOf(taxId));
    }

    public QueryResult<TaxonomyEntry> search(TaxonomyRequestDTO request) {
        SimpleQuery query = basicService.createSolrQuery(request, facetConfig, taxonomySortClause, defaultSearchHandler);
        return basicService.search(query, request.getCursor(), request.getSize());
    }

}
