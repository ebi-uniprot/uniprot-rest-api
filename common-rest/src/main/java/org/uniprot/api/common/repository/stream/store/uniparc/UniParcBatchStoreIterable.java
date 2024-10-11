package org.uniprot.api.common.repository.stream.store.uniparc;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class UniParcBatchStoreIterable extends BatchIterable<UniParcEntry> {
    private static final int MAX_NUMBER_CROSS_REF_ALLOWED = 50_000;
    private final UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;
    private final UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossRefStoreClient;
    private final RetryPolicy<Object> retryPolicy;
    private final UniParcCrossReferenceStoreConfigProperties crossRefConfigProperties;

    public UniParcBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossRefStoreClient,
            UniParcCrossReferenceStoreConfigProperties crossRefConfigProperties) {
        super(sourceIterator, batchSize);
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.retryPolicy = retryPolicy;
        this.uniParcCrossRefStoreClient = uniParcCrossRefStoreClient;
        this.crossRefConfigProperties = crossRefConfigProperties;
    }

    @Override
    protected List<UniParcEntry> convertBatch(List<String> batch) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to voldemort server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> getUniParcEntries(batch));
    }

    private List<UniParcEntry> getUniParcEntries(List<String> batch) {
        return this.uniParcLightStoreClient.getEntries(batch).stream()
                .map(this::getUniParcEntry)
                .toList();
    }

    private UniParcEntry getUniParcEntry(UniParcEntryLight uniParcEntryLight) {
        int storePageCount = getCrossRefStorePageCount(uniParcEntryLight);
        String uniParcId = uniParcEntryLight.getUniParcId();
        List<UniParcCrossReference> crossReferences =
                IntStream.range(0, storePageCount)
                        .mapToObj(batch -> uniParcId + "_" + batch)
                        .map(
                                batchId ->
                                        Failsafe.with(retryPolicy)
                                                .get(
                                                        () ->
                                                                this.uniParcCrossRefStoreClient
                                                                        .getEntry(batchId)))
                        .flatMap(Optional::stream)
                        .flatMap(pair -> pair.getValue().stream())
                        .toList();
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcId).sequence(uniParcEntryLight.getSequence());
        builder.sequenceFeaturesSet(uniParcEntryLight.getSequenceFeatures());
        // populate cross-references from its own store
        builder.uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }

    private int getCrossRefStorePageCount(UniParcEntryLight uniParcEntryLight) {
        int groupSize = this.crossRefConfigProperties.getGroupSize();
        // We support the full UniParc object via async download in XML format,
        // but we limit it to a maximum of 50k cross-references per UniParc entry to avoid OOM
        int crossReferencesCount = Math.min(MAX_NUMBER_CROSS_REF_ALLOWED, uniParcEntryLight.getCrossReferenceCount());
        // Calculate the number of batches required
        return crossReferencesCount / groupSize + (crossReferencesCount % groupSize == 0 ? 0 : 1);
    }
}
