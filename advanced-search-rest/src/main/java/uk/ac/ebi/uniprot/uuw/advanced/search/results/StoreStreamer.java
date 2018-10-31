package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Builder;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.data.domain.Sort;
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

/**
 * The purpose of this class is to stream results from a data-store (e.g., Voldemort / Solr's stored fields).
 * Clients of this class need not know what store they need to access. They need only provide the query that
 * needs answering, in addition to the sortable fields.
 * <p>
 * Created 22/08/18
 *
 * @author Edd
 */
@Builder
public class StoreStreamer<T> {
    private VoldemortClient<T> storeClient;
    private int streamerBatchSize;
    private String id;
    private String defaultsField;
    private Function<String, T> defaultsConverter;
    private TupleStreamTemplate tupleStreamTemplate;

    @SuppressWarnings("unchecked")
    public Stream<T> idsToStoreStream(String query, Sort sort) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(query, id, sort);
            tupleStream.open();

            BatchStoreIterable<T> batchStoreIterable = new BatchStoreIterable<>(
                    new TupleStreamIterable(tupleStream, id),
                    storeClient,
                    streamerBatchSize);
            return StreamSupport.stream(batchStoreIterable.spliterator(), false).flatMap(Collection::stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Stream<String> idsStream(String query, Sort sort) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(query, id, sort);
            tupleStream.open();
            return StreamSupport.stream(new TupleStreamIterable(tupleStream, id).spliterator(), false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Stream<T> defaultFieldStream(String query, Sort sort) {
        try {
            TupleStream tupleStream = tupleStreamTemplate.create(query, defaultsField, sort);
            tupleStream.open();

            TupleStreamIterable sourceIterable = new TupleStreamIterable(tupleStream, defaultsField);
            BatchIterable<T> batchIterable = new BatchIterable<T>(sourceIterable, streamerBatchSize) {
                @Override
                List<T> convertBatch(List<String> batch) {
                    return batch.stream().map(defaultsConverter).collect(Collectors.toList());
                }
            };

            return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
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
