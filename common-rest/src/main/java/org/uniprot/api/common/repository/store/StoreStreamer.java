package org.uniprot.api.common.repository.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.store.datastore.UniProtStoreClient;

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
public class StoreStreamer<T> {
    private final UniProtStoreClient<T> storeClient;
    private final RDFService<String> rdfStoreClient;
    private final TupleStreamTemplate tupleStreamTemplate;
    private final StreamerConfigProperties streamConfig;
    private final int rdfBatchSize; // number of accession in rdf rest request
    private final RetryPolicy<Object> storeFetchRetryPolicy;
    private final RetryPolicy<Object> rdfFetchRetryPolicy; // retry policy for RDF rest call
    private final String rdfProlog; // rdf prefix

    @SuppressWarnings("squid:S2095")
    public Stream<T> idsToStoreStream(SolrRequest solrRequest) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(solrRequest);
            tupleStream.open();

            BatchStoreIterable<T> batchStoreIterable =
                    new BatchStoreIterable<>(
                            new TupleStreamIterable(tupleStream, streamConfig.getIdFieldName()),
                            storeClient,
                            storeFetchRetryPolicy,
                            streamConfig.getStoreBatchSize());
            return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                    .flatMap(Collection::stream)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Stream<String> idsStream(SolrRequest solrRequest) {
        return fetchIds(solrRequest);
    }

    public Stream<String> idsToRDFStoreStream(SolrRequest solrRequest) {
        Stream<String> idsStream = fetchIds(solrRequest);

        BatchRDFStoreIterable<String> batchRDFStoreIterable =
                new BatchRDFStoreIterable<>(
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
                Stream.of(rdfProlog),
                Stream.concat(rdfStringStream, Stream.of(RDFService.RDF_CLOSE_TAG)));
    }

    public Stream<T> streamEntries(List<String> accessions) {
        BatchStoreIterable<T> batchStoreIterable =
                new BatchStoreIterable<>(
                        accessions,
                        storeClient,
                        storeFetchRetryPolicy,
                        streamConfig.getStoreBatchSize());
        return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .onClose(() -> log.debug("Finished streaming entries."));
    }

    @SuppressWarnings("squid:S2095")
    private Stream<String> fetchIds(SolrRequest solrRequest) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(solrRequest);
            tupleStream.open();
            return StreamSupport.stream(
                            new TupleStreamIterable(tupleStream, streamConfig.getIdFieldName())
                                    .spliterator(),
                            false)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void closeTupleStream(TupleStream tupleStream) {
        try {
            tupleStream.close();
            log.info("TupleStream closed: {}", tupleStream.getStreamNodeId());
        } catch (IOException e) {
            String message = "Error when closing TupleStream";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    // iterable for RDF streaming
    private static class BatchRDFStoreIterable<T> extends BatchIterable<T> {
        private final RDFService<T> storeClient;
        private final RetryPolicy<Object> retryPolicy;

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
                    .onFailure(
                            throwable ->
                                    log.error(
                                            "Call to RDF server failed for accessions {} with error {}",
                                            batch,
                                            throwable.getFailure().getMessage()))
                    .get(() -> storeClient.getEntries(batch));
        }
    }
}
