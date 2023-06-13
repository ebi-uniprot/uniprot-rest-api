package org.uniprot.api.idmapping.service.store.impl;

import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
@Slf4j
public class UniRefBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight> {

    public UniRefBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
    }

    public UniRefBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy) {
        super(sourceIterator, batchSize, storeClient, retryPolicy);
    }

    @Override
    protected UniRefEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniRefEntryLight> idEntryMap) {
        return UniRefEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.getOrDefault(mId.getTo(), null))
                .build();
    }

    @Override
    protected String getEntryId(UniRefEntryLight entry) {
        return entry.getId().getValue();
    }

    @Override
    protected void logTiming(int batchSize, long start, long end) {
        log.info(
                "Total {} UniRef entries fetched from voldemort in {} ms",
                batchSize,
                (end - start));
    }
}
