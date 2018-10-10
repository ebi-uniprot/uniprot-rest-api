package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.solr.client.solrj.io.stream.TupleStream;
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
 * Created 22/08/18
 *
 * @author Edd
 */
public class StoreStreamer<T> {
    private final VoldemortClient<T> storeClient;
    private final int streamerBatchSize;
    private final String id;
    private final String defaultsField;
    private final Function<String, T> defaultsConverter;

    StoreStreamer(VoldemortClient<T> storeClient, int streamerBatchSize, String id, String defaultsField, Function<String, T> defaultsConverter) {
        this.storeClient = storeClient;
        this.streamerBatchSize = streamerBatchSize;
        this.id = id;
        this.defaultsField = defaultsField;
        this.defaultsConverter = defaultsConverter;
    }

    @SuppressWarnings("unchecked")
    public Stream<Collection<T>> idsToStoreStream(TupleStream tupleStream) {
        BatchStoreIterable<T> batchStoreIterable = new BatchStoreIterable<>(
                new TupleStreamIterable(tupleStream, id),
                storeClient,
                streamerBatchSize);
        return StreamSupport.stream(batchStoreIterable.spliterator(), false);
    }

    public Stream<String> idsStream(TupleStream tupleStream) {
        return StreamSupport.stream(new TupleStreamIterable(tupleStream, id).spliterator(), false);
    }

    public Stream<Collection<T>> defaultFieldStream(TupleStream tupleStream) {
        TupleStreamIterable sourceIterable = new TupleStreamIterable(tupleStream, defaultsField);
        BatchIterable<T> batchIterable = new BatchIterable<T>(sourceIterable, streamerBatchSize) {
            @Override
            List<T> convertBatch(List<String> batch) {
                return batch.stream().map(defaultsConverter).collect(Collectors.toList());
            }
        };

        return StreamSupport.stream(batchIterable.spliterator(), false);
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
