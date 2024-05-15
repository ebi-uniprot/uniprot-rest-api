package org.uniprot.api.idmapping.common.service.store.impl;

import java.util.Iterator;
import java.util.Map;

import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
@Slf4j
public class UniParcBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry> {

    public UniParcBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniParcEntry> storeClient,
            RetryPolicy<Object> retryPolicy) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
    }

    public UniParcBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<UniParcEntry> storeClient,
            RetryPolicy<Object> retryPolicy) {
        super(sourceIterator, batchSize, storeClient, retryPolicy);
    }

    @Override
    protected UniParcEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniParcEntry> idEntryMap) {
        return UniParcEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.getOrDefault(mId.getTo(), null))
                .build();
    }

    @Override
    protected String getEntryId(UniParcEntry entry) {
        return entry.getUniParcId().getValue();
    }

    @Override
    protected void logTiming(int batchSize, long start, long end) {
        log.info(
                "Total {} UniParc entries fetched from voldemort in {} ms",
                batchSize,
                (end - start));
    }
}
