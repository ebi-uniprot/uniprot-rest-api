package org.uniprot.api.idmapping.common.service.store.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.util.Utils;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
@Slf4j
public class UniParcLightBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniParcEntryLightPair, UniParcEntryLight> {

    private final UniParcCrossReferenceLazyLoader lazyLoader;

    private final String fields;

    public UniParcLightBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            UniParcCrossReferenceLazyLoader lazyLoader,
            String fields) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
        this.lazyLoader = lazyLoader;
        this.fields = fields;
    }

    public UniParcLightBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            UniParcCrossReferenceLazyLoader lazyLoader,
            String fields) {
        super(sourceIterator, batchSize, storeClient, retryPolicy);
        this.lazyLoader = lazyLoader;
        this.fields = fields;
    }

    @Override
    protected UniParcEntryLightPair convertToPair(
            IdMappingStringPair mId, Map<String, UniParcEntryLight> idEntryMap) {
        UniParcEntryLight lightEntry = idEntryMap.getOrDefault(mId.getTo(), null);
        UniParcEntryLightPair.UniParcEntryLightPairBuilder builder =
                UniParcEntryLightPair.builder().from(mId.getFrom());
        List<String> lazyFields = lazyLoader.getLazyFields(fields);
        if (Utils.notNullNotEmpty(lazyFields)) {
            lightEntry = lazyLoader.populateLazyFields(lightEntry, lazyFields);
        }
        return builder.to(lightEntry).build();
    }

    @Override
    protected String getEntryId(UniParcEntryLight entry) {
        return entry.getUniParcId();
    }

    @Override
    protected void logTiming(int batchSize, long start, long end) {
        log.info(
                "Total {} UniParc entries fetched from voldemort in {} ms",
                batchSize,
                (end - start));
    }
}
