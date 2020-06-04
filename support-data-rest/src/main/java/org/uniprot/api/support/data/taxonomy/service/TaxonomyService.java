package org.uniprot.api.support.data.taxonomy.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

@Service
@Import(TaxonomyQueryBoostsConfig.class)
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    private SearchFieldConfig searchFieldConfig;

    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyFacetConfig facetConfig,
            TaxonomyEntryConverter converter,
            TaxonomySortClause taxonomySortClause,
            QueryBoosts taxonomyQueryBoosts) {

        super(repository, converter, taxonomySortClause, taxonomyQueryBoosts, facetConfig);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.TAXONOMY);
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    protected String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }
}
