package org.uniprot.api.proteome.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.proteome.ProteomeDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.PROTEOME, "content", "upid", emptyList());

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            ProteomeFacetConfig facetConfig,
            ProteomeSortClause solrSortClause) {
        super(
                repository,
                new ProteomeEntryConverter(),
                solrSortClause,
                handlerSupplier.get(),
                facetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.PROTEOME.getField("upid").getName();
    }
}
