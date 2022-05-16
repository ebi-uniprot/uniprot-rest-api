package org.uniprot.api.common.repository.stream.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamIterable;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.DocumentIdStream;
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
    private final TupleStreamTemplate tupleStreamTemplate;
    private final StreamerConfigProperties streamConfig;
    private final RetryPolicy<Object> storeFetchRetryPolicy;
    private final DocumentIdStream documentIdStream;

    @SuppressWarnings("squid:S2095")
    public Stream<T> idsToStoreStream(SolrRequest solrRequest) {
        TupleStream tupleStream = tupleStreamTemplate.create(solrRequest);
        try {
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
            closeTupleStream(tupleStream);
            throw new IllegalStateException(e);
        }
    }

    public Stream<String> idsStream(SolrRequest solrRequest) {
        return documentIdStream.fetchIds(solrRequest);
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
