package org.uniprot.api.common.repository.stream.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class BatchIterable<T> implements Iterable<Collection<T>> {
    /** Temp code * */
    public static Set<String> FILTERED_UPIDS;

    static {
        FILTERED_UPIDS = new HashSet<>();
        ClassLoader classLoader = BatchIterable.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("upids_10k.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                FILTERED_UPIDS.add(line.strip());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        System.out.println("Loaded UPI ids count: " + FILTERED_UPIDS.size());
    }
    /** Temp code ends * */
    private final Iterator<String> sourceIterator;

    private final int batchSize;
    private final String idField;

    public BatchIterable(Iterable<String> sourceIterable, int batchSize) {
        this.batchSize = batchSize;
        this.sourceIterator = sourceIterable.iterator();
        if (sourceIterable instanceof TupleStreamIterable) {
            this.idField = ((TupleStreamIterable) sourceIterable).getId();
        } else {
            this.idField = "";
        }
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
                        String idValue = sourceIterator.next();
                        if (!("upi".equals(idField) && FILTERED_UPIDS.contains(idValue))) {
                            batch.add(idValue);
                        }
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
