package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    public StoreStreamer(VoldemortClient<T> storeClient, int streamerBatchSize, String id) {
        this.storeClient = storeClient;
        this.streamerBatchSize = streamerBatchSize;
        this.id = id;
    }

    public Stream<Collection<T>> searchToStoreStream(TupleStream tupleStream) {
        BatchStoreIterable<T> batchStoreIterable = new BatchStoreIterable<>(
                new TupleStreamIterable(tupleStream, id),
                storeClient,
                streamerBatchSize);
        return StreamSupport.stream(batchStoreIterable.spliterator(), false);
    }

    private static class BatchStoreIterable<T> implements Iterable<Collection<T>> {
        private final Iterator<String> sourceIterator;
        private final int batchSize;
        private final VoldemortClient<T> storeClient;

        BatchStoreIterable(Iterable<String> sourceIterable, VoldemortClient<T> storeClient, int batchSize) {
            this.batchSize = batchSize;
            this.sourceIterator = sourceIterable.iterator();
            this.storeClient = storeClient;
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
                    return storeClient.getEntries(batch);
                }
            };
        }
    }
}
