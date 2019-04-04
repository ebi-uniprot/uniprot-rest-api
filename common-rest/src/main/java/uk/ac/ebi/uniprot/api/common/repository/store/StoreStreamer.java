package uk.ac.ebi.uniprot.api.common.repository.store;

import lombok.Builder;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.slf4j.Logger;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The purpose of this class is to stream results from a data-store (e.g., Voldemort / Solr's stored fields).
 * Clients of this class need not know what store they need to access. They need only provide the query that
 * needs answering, in addition to the sortable fields.
 *
 * Created 22/08/18
 *
 * @author Edd
 */
@Builder
public class StoreStreamer<T> {
    private static final Logger LOGGER = getLogger(StoreStreamer.class);
    private VoldemortClient<T> storeClient;
    private int streamerBatchSize;
    private String id;
    private String defaultsField;
    private Function<String, T> defaultsConverter;
    private TupleStreamTemplate tupleStreamTemplate;

    public Stream<T> idsToStoreStream(StreamRequest request) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(request, id);
            tupleStream.open();

            BatchStoreIterable<T> batchStoreIterable = new BatchStoreIterable<>(
                    new TupleStreamIterable(tupleStream, id),
                    storeClient,
                    streamerBatchSize);
            return StreamSupport
                    .stream(batchStoreIterable.spliterator(), false)
                    .flatMap(Collection::stream)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Stream<String> idsStream(StreamRequest request) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(request, id);
            tupleStream.open();
            return StreamSupport
                    .stream(new TupleStreamIterable(tupleStream, id).spliterator(), false)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Stream<T> defaultFieldStream(StreamRequest request) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(request, defaultsField);
            tupleStream.open();

            TupleStreamIterable sourceIterable = new TupleStreamIterable(tupleStream, defaultsField);
            BatchIterable<T> batchIterable = new BatchIterable<T>(sourceIterable, streamerBatchSize) {
                @Override
                List<T> convertBatch(List<String> batch) {
                    return batch.stream().map(defaultsConverter).collect(Collectors.toList());
                }
            };

            return StreamSupport
                    .stream(batchIterable.spliterator(), false)
                    .flatMap(Collection::stream)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void closeTupleStream(TupleStream tupleStream) {
        try {
            tupleStream.close();
            LOGGER.info("TupleStream closed: {}", tupleStream.getStreamNodeId());
        } catch (IOException e) {
            String message = "Error when closing TupleStream";
            LOGGER.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private static class BatchStoreIterable<T> extends BatchIterable<T> {
        private VoldemortClient<T> storeClient;
        private RetryPolicy retryPolicy;

        BatchStoreIterable(Iterable<String> sourceIterable, VoldemortClient<T> storeClient, int batchSize) {
            super(sourceIterable, batchSize);
            this.storeClient = storeClient;
            this.retryPolicy = new RetryPolicy()
                    .retryOn(IOException.class)
                    .withDelay(500, TimeUnit.MILLISECONDS)
                    .withMaxRetries(5);
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
