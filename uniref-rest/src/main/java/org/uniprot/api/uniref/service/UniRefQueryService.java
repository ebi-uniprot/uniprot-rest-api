package org.uniprot.api.uniref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.uniref.UniRefDocument;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.UniRefField.Search;

import java.util.function.Supplier;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
public class UniRefQueryService extends StoreStreamerSearchService<UniRefDocument, UniRefEntry> {
    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.UNIREF, "content", "upi", Search.getBoostFields());

    @Autowired
    public UniRefQueryService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefSortClause uniRefSortClause,
            UniRefStoreClient entryStore,
            UniRefQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefDocument, UniRefEntry> storeStreamer) {
        super(
                repository,
                uniRefQueryResultConverter,
                uniRefSortClause,
                handlerSupplier.get(),
                facetConfig,
                storeStreamer);
    }

    @Override
    public UniRefEntry findByUniqueId(String uniqueId, String fields) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.UNIREF.getField("id").getName();
    }
}
