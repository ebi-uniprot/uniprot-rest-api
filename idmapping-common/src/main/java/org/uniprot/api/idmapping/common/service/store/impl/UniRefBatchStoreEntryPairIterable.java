package org.uniprot.api.idmapping.common.service.store.impl;

import static org.uniprot.api.uniref.common.service.light.UniRefEntryLightUtils.*;
import static org.uniprot.api.uniref.common.service.light.UniRefEntryLightUtils.cleanMemberId;

import java.util.Iterator;
import java.util.Map;

import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
@Slf4j
public class UniRefBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight> {

    private final boolean complete;

    public UniRefBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            boolean complete) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
        this.complete = complete;
    }

    public UniRefBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            boolean complete) {
        super(sourceIterator, batchSize, storeClient, retryPolicy);
        this.complete = complete;
    }

    @Override
    protected UniRefEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniRefEntryLight> idEntryMap) {
        UniRefEntryPair.UniRefEntryPairBuilder builder = UniRefEntryPair.builder();
        builder.from(mId.getFrom());
        UniRefEntryLight to = idEntryMap.get(mId.getTo());
        if (to != null) {
            if (this.complete) {
                builder.to(cleanMemberId(to));
            } else {
                builder.to(removeOverLimitAndCleanMemberId(to));
            }
        }
        return builder.build();
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
