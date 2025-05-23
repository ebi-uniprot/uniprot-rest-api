package org.uniprot.api.uniparc.common.repository.store.stream;

import java.util.List;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.uniparc.common.service.filter.UniParcProteomeIdFilter;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class UniParcFastaBatchStoreIterable extends BatchStoreIterable<UniParcEntry> {

    private final UniProtStoreClient<UniParcEntryLight> storeClient;

    private final UniParcCrossReferenceService uniParcCrossReferenceService;

    private final RetryPolicy<Object> retryPolicy;

    private final String proteomeId;

    public UniParcFastaBatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniParcCrossReferenceService uniParcCrossReferenceService,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            String proteomeId) {
        super(sourceIterable, null, retryPolicy, batchSize);
        this.storeClient = storeClient;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
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
        return entries.stream().map(this::mapToUniParcEntry).toList();
    }

    private UniParcEntry mapToUniParcEntry(UniParcEntryLight light) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(light.getUniParcId()).sequence(light.getSequence());
        // populate cross-references from its own store
        Stream<UniParcCrossReference> crossReferences =
                uniParcCrossReferenceService.getCrossReferences(light, true);
        UniParcProteomeIdFilter proteomeIdFilter = new UniParcProteomeIdFilter();
        crossReferences = crossReferences.filter(xRef -> proteomeIdFilter.test(xRef, proteomeId));
        builder.uniParcCrossReferencesSet(crossReferences.toList());
        return builder.build();
    }
}
