package org.uniprot.api.idmapping.service.store.impl;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

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
