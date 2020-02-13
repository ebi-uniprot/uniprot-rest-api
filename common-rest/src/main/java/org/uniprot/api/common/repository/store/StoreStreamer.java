package org.uniprot.api.common.repository.store;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.web.client.ResourceAccessException;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.document.Document;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    private UniProtStoreClient<T> storeClient;
    private RDFService<String> rdfStoreClient;
    private SolrQueryRepository<D> repository;
    private Function<D, String> documentToId;
    private int storeBatchSize;
    private int rdfBatchSize; // number of accession in rdf rest request
    private RetryPolicy<Object> storeFetchRetryPolicy;
    private RetryPolicy<Object> rdfFetchRetryPolicy; // retry policy for RDF rest call

    public Stream<T> idsToStoreStream(SolrRequest solrRequest) {
        Stream<String> idsStream = fetchIds(solrRequest);

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

    public Stream<String> idsStream(SolrRequest solrRequest) {
        return fetchIds(solrRequest);
    }

    public Stream<String> idsToRDFStoreStream(SolrRequest solrRequest) {
        Stream<String> idsStream = fetchIds(solrRequest);

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

    private Stream<String> fetchIds(SolrRequest solrRequest) {

        Stream<String> idsStream =
                repository.getAll(solrRequest).map(documentToId).limit(solrRequest.getTotalRows());

        return idsStream;
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
            return Failsafe.with(retryPolicy)
                    .onFailure(throwable -> log.error("Call to RDF server failed for accessions {} with error {}", batch,
                            throwable.getFailure().getMessage()))
                    .get(() -> storeClient.getEntries(batch));
        }
    }
}
