package org.uniprot.api.uniparc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.UniParcField.Search;

import java.util.function.Supplier;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
public class UniParcQueryService extends BasicSearchService<UniParcDocument, UniParcEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.UNIPARC, "content", "upi", Search.getBoostFields());

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
        return Search.upi.name();
    }
}
