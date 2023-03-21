package org.uniprot.api.common.repository.stream.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class BatchIterable<T> implements Iterable<Collection<T>> {
    private final Iterator<String> sourceIterator;
    private final int batchSize;

    public BatchIterable(Iterator<String> sourceIterator, int batchSize) {
        this.batchSize = batchSize;
        this.sourceIterator = sourceIterator;
    }

    public BatchIterable(Iterable<String> sourceIterable, int batchSize) {
        this(sourceIterable.iterator(), batchSize);
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

    protected abstract List<T> convertBatch(List<String> batch);
}
