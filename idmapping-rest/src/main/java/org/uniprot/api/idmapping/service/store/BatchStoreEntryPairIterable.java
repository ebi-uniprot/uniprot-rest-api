package org.uniprot.api.idmapping.service.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
public abstract class BatchStoreEntryPairIterable<T extends EntryPair<S>, S>
        implements Iterable<Collection<T>> {
    private final Iterator<IdMappingStringPair> sourceIterator;
    private final int batchSize;
    private final UniProtStoreClient<S> storeClient;
    private final RetryPolicy<Object> retryPolicy;

    protected BatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<S> storeClient,
            RetryPolicy<Object> retryPolicy) {
        this.batchSize = batchSize;
        this.sourceIterator = sourceIterable.iterator();
        this.storeClient = storeClient;
        this.retryPolicy = retryPolicy;
    }

    public BatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<S> storeClient,
            RetryPolicy<Object> retryPolicy) {
        this.batchSize = batchSize;
        this.sourceIterator = sourceIterator;
        this.storeClient = storeClient;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Iterator<Collection<T>> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return sourceIterator.hasNext();
            }

            @Override
            public List<T> next() {
                Set<String> tosBatch = new HashSet<>(batchSize);
                List<IdMappingStringPair> batch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize; i++) {
                    if (sourceIterator.hasNext()) {
                        IdMappingStringPair nextPair = sourceIterator.next();
                        tosBatch.add(nextPair.getTo());
                        batch.add(nextPair);
                    } else {
                        break;
                    }
                }

                return convertBatch(tosBatch, batch);
            }
        };
    }

    protected List<T> convertBatch(Set<String> tos, List<IdMappingStringPair> batch) {
        long start = System.currentTimeMillis();
        List<S> entries = getEntriesFromStore(tos);
        long end = System.currentTimeMillis();
        if (tos.size() >= 100) { // temp code block
            logTiming(tos.size(), start, end);
        }
        // entry -> map <entryId, entry>
        Map<String, S> idEntryMap =
                entries.stream().collect(Collectors.toMap(this::getEntryId, Function.identity()));

        // id mapping pairs -> Ts
        return batch.stream()
                .map(mId -> convertToPair(mId, idEntryMap))
                .filter(pair -> pair.getTo() != null)
                .collect(Collectors.toList());
    }

    protected List<S> getEntriesFromStore(Set<String> tos) {
        return Failsafe.with(retryPolicy).get(() -> storeClient.getEntries(tos));
    }

    protected abstract T convertToPair(IdMappingStringPair mId, Map<String, S> idEntryMap);

    protected abstract String getEntryId(S entry);

    protected abstract void logTiming(
            int batchSize, long start, long end); // temp method, remove after testing
}
