package org.uniprot.api.taxonomy.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.TAXONOMY, "content", "id", emptyList());

    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyFacetConfig facetConfig,
            TaxonomyEntryConverter converter,
            TaxonomySortClause taxonomySortClause) {

        super(repository, converter, taxonomySortClause, handlerSupplier.get(), facetConfig);
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.TAXONOMY.getField("id").getName();
    }
}
