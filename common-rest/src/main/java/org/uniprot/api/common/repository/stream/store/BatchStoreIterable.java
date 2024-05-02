package org.uniprot.api.common.repository.stream.store;

import java.util.Iterator;
import java.util.List;

import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class BatchStoreIterable<T> extends BatchIterable<T> {
    private final UniProtStoreClient<T> storeClient;
    private final RetryPolicy<Object> retryPolicy;

    public BatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<T> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize) {
        super(sourceIterable, batchSize);
        this.storeClient = storeClient;
        this.retryPolicy = retryPolicy;
    }

    public BatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<T> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize) {
        super(sourceIterator, batchSize);
        this.storeClient = storeClient;
        this.retryPolicy = retryPolicy;
    }

    @Override
    protected List<T> convertBatch(List<String> batch) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to voldemort server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> storeClient.getEntries(batch));
    }
}
