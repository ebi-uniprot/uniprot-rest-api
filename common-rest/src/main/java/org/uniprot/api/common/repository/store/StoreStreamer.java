package org.uniprot.api.common.repository.store;

import static java.util.stream.StreamSupport.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.document.Document;

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
public class StoreStreamer<D extends Document, T> {
    private static final int IDS_BATCH_SIZE = 100_000;
    private UniProtStoreClient<T> storeClient;
    private SolrQueryRepository<D> repository;
    private Function<D, String> documentToId;
    private int searchBatchSize;
    private int storeBatchSize;
    private RetryPolicy<Object> storeFetchRetryPolicy;

    public Stream<T> idsToStoreStream(SolrRequest origRequest) {
        Stream<String> idsStream = fetchIds(origRequest, searchBatchSize);

        StoreStreamer.BatchStoreIterable<T> batchStoreIterable =
                new StoreStreamer.BatchStoreIterable<>(
                        idsStream::iterator, storeClient, storeFetchRetryPolicy, storeBatchSize);
        return stream(batchStoreIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .onClose(
                        () ->
                                log.debug(
                                        "Finished streaming over search results and fetching from key/value store."));
    }

    public Stream<String> idsStream(SolrRequest origRequest) {
        return fetchIds(origRequest, IDS_BATCH_SIZE);
    }

    private Stream<String> fetchIds(SolrRequest origRequest, int searchBatchSize) {
        int limit = origRequest.getRows();
        int fetchSize = searchBatchSize;
        if (limit < searchBatchSize) {
            fetchSize = limit;
        }
        SolrRequest request = setSolrBatchSize(origRequest, fetchSize);

        Stream<String> idsStream = repository.getAll(request).map(documentToId);
        if (limit >= 0) {
            idsStream = idsStream.limit(limit);
        }
        return idsStream;
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
