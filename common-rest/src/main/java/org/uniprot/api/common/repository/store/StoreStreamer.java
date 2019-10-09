package org.uniprot.api.common.repository.store;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

/**
 * The purpose of this class is to stream results from a data-store, e.g., Voldemort. Clients of
 * this class need not know what store they need to access. They need only provide the request that
 * needs answering.
 *
 * <p>Created 22/08/18
 *
 * @author Edd
 */
@Builder
@Slf4j
public class StoreStreamer<D, T> {
    private UniProtStoreClient<T> storeClient;
    private SolrQueryRepository<D> repository;
    private Function<D, String> documentToId;
    private int searchBatchSize;
    private int storeBatchSize;
    private RetryPolicy<Object> storeFetchRetryPolicy;

    public Stream<T> idsToStoreStream(SolrRequest origRequest) {
        int limit = origRequest.getRows();
        SolrRequest request = setSolrBatchSize(origRequest, searchBatchSize);
        Stream<String> idsStream =
                stream(
                                spliteratorUnknownSize(
                                        repository.getAll(request), Spliterator.ORDERED),
                                false)
                        .map(documentToId)
                        .limit(limit);

        StoreStreamer.BatchStoreIterable<T> batchStoreIterable =
                new StoreStreamer.BatchStoreIterable<>(
                        idsStream::iterator, storeClient, storeFetchRetryPolicy, storeBatchSize);
        return stream(batchStoreIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .onClose(
                        () ->
                                log.info(
                                        "Finished streaming over search results and fetching from key/value store."));
    }

    public Stream<String> idsStream(SolrRequest origRequest) {
        int limit = origRequest.getRows();
        SolrRequest request = setSolrBatchSize(origRequest, searchBatchSize);
        return stream(
                        spliteratorUnknownSize(repository.getAll(request), Spliterator.ORDERED),
                        false)
                .map(documentToId)
                .limit(limit);
    }

    private SolrRequest setSolrBatchSize(SolrRequest origRequest, int searchBatchSize) {
        return origRequest.toBuilder().rows(searchBatchSize).build();
    }

    private static class BatchStoreIterable<T> extends BatchIterable<T> {
        private UniProtStoreClient<T> storeClient;
        private RetryPolicy<Object> retryPolicy;

        BatchStoreIterable(
                Iterable<String> sourceIterable,
                UniProtStoreClient<T> storeClient,
                RetryPolicy<Object> retryPolicy,
                int batchSize) {
            super(sourceIterable, batchSize);
            this.storeClient = storeClient;
            this.retryPolicy = retryPolicy;
        }

        @Override
        List<T> convertBatch(List<String> batch) {
            return Failsafe.with(retryPolicy).get(() -> storeClient.getEntries(batch));
        }
    }

    private abstract static class BatchIterable<T> implements Iterable<Collection<T>> {
        private final Iterator<String> sourceIterator;
        private final int batchSize;

        BatchIterable(Iterable<String> sourceIterable, int batchSize) {
            this.batchSize = batchSize;
            this.sourceIterator = sourceIterable.iterator();
        }

        @Override
        public Iterator<Collection<T>> iterator() {
            return new Iterator<Collection<T>>() {
                @Override
                public boolean hasNext() {
                    return sourceIterator.hasNext();
                }

                @Override
                public List<T> next() {
                    List<String> batch = new ArrayList<>(batchSize);
                    for (int i = 0; i < batchSize; i++) {
                        if (sourceIterator.hasNext()) {
                            batch.add(sourceIterator.next());
                        } else {
                            break;
                        }
                    }

                    return convertBatch(batch);
                }
            };
        }

        abstract List<T> convertBatch(List<String> batch);
    }
}
