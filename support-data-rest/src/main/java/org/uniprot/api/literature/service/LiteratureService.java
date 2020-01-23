package org.uniprot.api.literature.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.literature.repository.LiteratureRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.field.LiteratureField;
import org.uniprot.store.search.field.UniProtSearchFields;

import java.util.function.Supplier;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
public class LiteratureService extends BasicSearchService<LiteratureDocument, LiteratureEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.LITERATURE,
                            "content",
                            "id",
                            LiteratureField.Search.getBoostFields());

    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            LiteratureFacetConfig facetConfig,
            LiteratureSortClause literatureSortClause) {
        super(repository, entryConverter, literatureSortClause, handlerSupplier.get(), facetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.LITERATURE.getField("id").getName();
    }
}
