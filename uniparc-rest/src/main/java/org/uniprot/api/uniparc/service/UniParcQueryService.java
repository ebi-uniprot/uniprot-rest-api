package org.uniprot.api.uniparc.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
public class UniParcQueryService extends BasicSearchService<UniParcDocument, UniParcEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.UNIPARC, "content", "upi", emptyList());

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcEntryConverter uniParcEntryConverter,
            UniParcSortClause solrSortClause) {

        super(
                repository,
                uniParcEntryConverter,
                solrSortClause,
                handlerSupplier.get(),
                facetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.UNIPARC.getField("upi").getName();
    }
}
