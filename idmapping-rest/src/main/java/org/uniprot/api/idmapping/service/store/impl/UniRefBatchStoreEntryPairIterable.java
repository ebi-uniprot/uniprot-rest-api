package org.uniprot.api.idmapping.service.store.impl;

import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.Map;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
public class UniRefBatchStoreEntryPairIterable extends BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight> {

    public UniRefBatchStoreEntryPairIterable(Iterable<IdMappingStringPair> sourceIterable,
                                                int batchSize,
                                                UniProtStoreClient<UniRefEntryLight> storeClient,
                                                RetryPolicy<Object> retryPolicy) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
    }

    @Override
    protected UniRefEntryPair convertToPair(IdMappingStringPair mId, Map<String, UniRefEntryLight> idEntryMap) {
        return UniRefEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniRefEntryLight entry) {
        return entry.getId().getValue();
    }
}
