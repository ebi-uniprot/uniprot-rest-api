package org.uniprot.api.common.repository.stream.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamIterable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
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
@Slf4j
public class StoreStreamer<T> {

    protected StoreStreamerConfig<T> config;

    public StoreStreamer(StoreStreamerConfig<T> config) {
        this.config = config;
    }

    public Stream<T> idsToStoreStream(SolrRequest solrRequest) {
        return idsToStoreStream(solrRequest, StoreRequest.builder().build());
    }

    @SuppressWarnings("squid:S2095")
    public Stream<T> idsToStoreStream(SolrRequest solrRequest, StoreRequest storeRequest) {
        TupleStream tupleStream = config.getTupleStreamTemplate().create(solrRequest);
        try {
            tupleStream.open();
            TupleStreamIterable iterable =
                    new TupleStreamIterable(tupleStream, config.getStreamConfig().getIdFieldName());
            BatchStoreIterable<T> batchStoreIterable =
                    getBatchStoreIterable(iterable, storeRequest);
            return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                    .flatMap(Collection::stream)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (Exception e) {
            closeTupleStream(tupleStream);
            throw new IllegalStateException(e);
        }
    }

    public Stream<String> idsStream(SolrRequest solrRequest) {
        return config.getDocumentIdStream().fetchIds(solrRequest);
    }

    public long getNoOfEntries(SolrRequest solrRequest) {
        return config.getTupleStreamTemplate().getNumberOfEntries(solrRequest);
    }

    public Stream<T> streamEntries(List<String> accessions) {
        return streamEntries(accessions, StoreRequest.builder().build());
    }

    public Stream<T> streamEntries(List<String> accessions, StoreRequest storeRequest) {
        BatchStoreIterable<T> batchStoreIterable = getBatchStoreIterable(accessions, storeRequest);
        return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .onClose(() -> log.debug("Finished streaming entries."));
    }

    protected BatchStoreIterable<T> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new BatchStoreIterable<>(
                iterableIds,
                config.getStoreClient(),
                config.getStoreFetchRetryPolicy(),
                config.getStreamConfig().getStoreBatchSize());
    }

    private void closeTupleStream(TupleStream tupleStream) {
        try {
            tupleStream.close();
            log.debug("TupleStream closed: {}", tupleStream.getStreamNodeId());
        } catch (IOException e) {
            String message = "Error when closing TupleStream";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }
}
