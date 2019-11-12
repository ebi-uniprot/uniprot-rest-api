package org.uniprot.api.common.repository.store;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.RDFService;
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
    private RDFService<String> rdfStoreClient;
    private SolrQueryRepository<D> repository;
    private Function<D, String> documentToId;
    private int searchBatchSize;
    private int storeBatchSize;
    private int rdfBatchSize; // number of accession in rdf rest request
    private RetryPolicy<Object> storeFetchRetryPolicy;
    private RetryPolicy<Object> rdfFetchRetryPolicy; // retry policy for RDF rest call

    public Stream<T> idsToStoreStream(SolrRequest origRequest) {
        Stream<String> idsStream = fetchIds(origRequest, searchBatchSize);

        StoreStreamer.BatchStoreIterable<T> batchStoreIterable =
                new StoreStreamer.BatchStoreIterable<>(
                        idsStream::iterator, storeClient, storeFetchRetryPolicy, storeBatchSize);
        return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .onClose(
                        () ->
                                log.debug(
                                        "Finished streaming over search results and fetching from key/value store."));
    }

    public Stream<String> idsStream(SolrRequest origRequest) {
        return fetchIds(origRequest, IDS_BATCH_SIZE); // FIXME make it configurable
    }

    public Stream<String> idsToRDFStoreStream(SolrRequest origRequest) {
        Stream<String> idsStream = fetchIds(origRequest, searchBatchSize);

        BatchRDFStoreIterable<String> batchRDFStoreIterable =
                new BatchRDFStoreIterable(
                        idsStream::iterator, rdfStoreClient, rdfFetchRetryPolicy, rdfBatchSize);

        Stream<String> rdfStringStream =
                StreamSupport.stream(batchRDFStoreIterable.spliterator(), false)
                        .flatMap(Collection::stream)
                        .onClose(
                                () ->
                                        log.debug(
                                                "Finished streaming over search results and fetching from RDF server."));

        // prepend rdf prolog then rdf data and then append closing rdf tag
        return Stream.concat(
                Stream.of(RDFService.RDF_PROLOG),
                Stream.concat(rdfStringStream, Stream.of(RDFService.RDF_CLOSE_TAG)));
    }

    private Stream<String> fetchIds(SolrRequest origRequest, int searchBatchSize) {
        int limit = origRequest.getTotalRows();
        int fetchSize = searchBatchSize;
        if (limit < searchBatchSize) {
            fetchSize = limit;
        }

        SolrRequest request = setSolrBatchSize(origRequest, fetchSize);
        Stream<String> idsStream = repository.getAll(request).map(documentToId).limit(limit);

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

    // iterable for RDF streaming
    private static class BatchRDFStoreIterable<T> extends BatchIterable<T> {
        private RDFService<T> storeClient;
        private RetryPolicy<Object> retryPolicy;

        BatchRDFStoreIterable(
                Iterable<String> sourceIterable,
                RDFService<T> storeClient,
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
}
