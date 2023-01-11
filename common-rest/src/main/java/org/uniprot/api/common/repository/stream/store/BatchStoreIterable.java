package org.uniprot.api.common.repository.stream.store;

import java.util.Iterator;
import java.util.List;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.store.datastore.UniProtStoreClient;

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
        return Failsafe.with(retryPolicy).get(() -> storeClient.getEntries(batch));
    }
}
