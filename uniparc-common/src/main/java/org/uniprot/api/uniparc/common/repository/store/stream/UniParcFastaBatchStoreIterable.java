package org.uniprot.api.uniparc.common.repository.store.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.uniparc.common.service.filter.UniParcProteomeIdFilter;
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
public class UniParcFastaBatchStoreIterable extends BatchStoreIterable<UniParcEntry> {

    private final UniProtStoreClient<UniParcEntryLight> storeClient;

    private final UniProtStoreClient<UniParcCrossReferencePair> crossRefStoreClient;

    private final UniParcCrossReferenceStoreConfigProperties storeConfigProperties;

    private final RetryPolicy<Object> retryPolicy;

    private final String proteomeId;

    public UniParcFastaBatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniProtStoreClient<UniParcCrossReferencePair> crossRefStoreClient,
            UniParcCrossReferenceStoreConfigProperties storeConfigProperties,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            String proteomeId) {
        super(sourceIterable, null, retryPolicy, batchSize);
        this.storeClient = storeClient;
        this.crossRefStoreClient = crossRefStoreClient;
        this.storeConfigProperties = storeConfigProperties;
        this.retryPolicy = retryPolicy;
        this.proteomeId = proteomeId;
    }

    public UniParcFastaBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniProtStoreClient<UniParcCrossReferencePair> crossRefStoreClient,
            UniParcCrossReferenceStoreConfigProperties storeConfigProperties,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            String proteomeId) {
        super(sourceIterator, null, retryPolicy, batchSize);
        this.storeClient = storeClient;
        this.crossRefStoreClient = crossRefStoreClient;
        this.storeConfigProperties = storeConfigProperties;
        this.retryPolicy = retryPolicy;
        this.proteomeId = proteomeId;
    }

    @Override
    protected List<UniParcEntry> convertBatch(List<String> batch) {
        List<UniParcEntryLight> entries =
                Failsafe.with(
                                retryPolicy.onRetry(
                                        e ->
                                                log.warn(
                                                        "Batch call to voldemort server failed. Failure #{}. Retrying...",
                                                        e.getAttemptCount())))
                        .get(() -> storeClient.getEntries(batch));
        return entries.stream().map(this::maptoUniParcEntry).toList();
    }

    private UniParcEntry maptoUniParcEntry(UniParcEntryLight light) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(light.getUniParcId()).sequence(light.getSequence());
        // populate cross-references from its own store
        Stream<UniParcCrossReference> crossReferences = getCrossReferences(light);
        UniParcProteomeIdFilter proteomeIdFilter = new UniParcProteomeIdFilter();
        crossReferences = crossReferences.filter(xRef -> proteomeIdFilter.apply(xRef, proteomeId));
        builder.uniParcCrossReferencesSet(crossReferences.toList());
        return builder.build();
    }

    private Stream<UniParcCrossReference> getCrossReferences(UniParcEntryLight uniParcEntryLight) {
        BatchStoreIterable<UniParcCrossReferencePair> batchIterable =
                new BatchStoreIterable<>(
                        generateUniParcCrossReferenceKeys(uniParcEntryLight),
                        this.crossRefStoreClient,
                        this.retryPolicy,
                        1);
        return StreamSupport.stream(batchIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .flatMap(pair -> pair.getValue().stream());
    }

    private List<String> generateUniParcCrossReferenceKeys(UniParcEntryLight uniParcEntryLight) {
        int xrefCount = uniParcEntryLight.getCrossReferenceCount();
        int groupSize = this.storeConfigProperties.getGroupSize();
        List<String> xrefKeys = new ArrayList<>();
        String uniParcId = uniParcEntryLight.getUniParcId();
        for (int i = 0, batchId = 0; i < xrefCount; i += groupSize, batchId++) {
            xrefKeys.add(uniParcId + "_" + batchId);
        }
        return xrefKeys;
    }
}
