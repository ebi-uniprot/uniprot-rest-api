package org.uniprot.api.idmapping.service.store.impl;

import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.Map;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
public class UniParcBatchStoreEntryPairIterable extends BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry> {

    public UniParcBatchStoreEntryPairIterable(Iterable<IdMappingStringPair> sourceIterable,
                                                int batchSize,
                                                UniProtStoreClient<UniParcEntry> storeClient,
                                                RetryPolicy<Object> retryPolicy) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
    }

    @Override
    protected UniParcEntryPair convertToPair(IdMappingStringPair mId, Map<String, UniParcEntry> idEntryMap) {
        return UniParcEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniParcEntry entry) {
        return entry.getUniParcId().getValue();
    }
}
