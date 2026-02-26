package org.uniprot.api.uniparc.common.repository.store.stream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, Long> requestTime;

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
        this.requestTime = new HashMap<>();
    }

    @Override
    protected List<UniParcEntry> convertBatch(List<String> batch) {
        // here we are getting a batch of uniparc entries via upis
        long startTime = System.currentTimeMillis();
        List<UniParcEntryLight> entries =
                Failsafe.with(
                                retryPolicy.onRetry(
                                        e ->
                                                log.warn(
                                                        "Batch call to voldemort server failed. Failure #{}. Retrying...",
                                                        e.getAttemptCount())))
                        .get(() -> storeClient.getEntries(batch));
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        String key = this.hashCode() + "_vd";
        this.requestTime.merge(key, elapsedTime, Long::sum);
        log.info(
                "Total time taken to get UniParc entries by request id {} is {} ms ",
                key,
                this.requestTime.get(key));
        return entries.stream().map(this::mapToUniParcEntry).toList();
    }

    private UniParcEntry mapToUniParcEntry(UniParcEntryLight light) {
        long startTime = System.currentTimeMillis();
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(light.getUniParcId()).sequence(light.getSequence());
        // populate cross-references from its own store
        Stream<UniParcCrossReference> crossReferences =
                uniParcCrossReferenceService.getCrossReferencesWithBatchSize(light, true);
        UniParcProteomeIdFilter proteomeIdFilter = new UniParcProteomeIdFilter();
        crossReferences = crossReferences.filter(xRef -> proteomeIdFilter.test(xRef, proteomeId));
        builder.uniParcCrossReferencesSet(crossReferences.toList());
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        String key = this.hashCode() + "_xref";
        this.requestTime.merge(key, elapsedTime, Long::sum);
        log.info(
                "Total time taken to get UniParc xrefs by request id {} is {} seconds ",
                key,
                this.requestTime.get(key) / 1000);
        return builder.build();
    }
}
