package org.uniprot.api.taxonomy.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
@Import(TaxonomyQueryBoostsConfig.class)
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyFacetConfig facetConfig,
            TaxonomyEntryConverter converter,
            TaxonomySortClause taxonomySortClause,
            QueryBoosts taxonomyQueryBoosts) {

        super(repository, converter, taxonomySortClause, taxonomyQueryBoosts, facetConfig);
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.TAXONOMY.getField("id").getName();
    }
}
