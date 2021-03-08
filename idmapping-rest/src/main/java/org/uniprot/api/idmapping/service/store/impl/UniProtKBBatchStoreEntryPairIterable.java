package org.uniprot.api.idmapping.service.store.impl;

import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.Map;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
public class UniProtKBBatchStoreEntryPairIterable extends BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry> {

    public UniProtKBBatchStoreEntryPairIterable(Iterable<IdMappingStringPair> sourceIterable,
                                                int batchSize,
                                                UniProtStoreClient<UniProtKBEntry> storeClient,
                                                RetryPolicy<Object> retryPolicy) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
    }

    @Override
    protected UniProtKBEntryPair convertToPair(IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKBEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniProtKBEntry entry) {
        return entry.getPrimaryAccession().getValue();
    }
}
