package org.uniprot.api.idmapping.service.store;

import java.util.*;
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
// TODO: 11/03/2021 needs tests
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

    @Override
    public Iterator<Collection<T>> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return sourceIterator.hasNext();
            }

            @Override
            public List<T> next() {
                List<IdMappingStringPair> batch = new ArrayList<>(batchSize);
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

    protected List<T> convertBatch(List<IdMappingStringPair> batch) {
        Set<String> toIds =
                batch.stream().map(IdMappingStringPair::getTo).collect(Collectors.toSet());
        List<S> entries = Failsafe.with(retryPolicy).get(() -> storeClient.getEntries(toIds));

        // accession -> entry map
        Map<String, S> idEntryMap =
                entries.stream().collect(Collectors.toMap(this::getEntryId, Function.identity()));

        // from -> uniprot entry
        return batch.stream()
                .filter(mId -> idEntryMap.containsKey(mId.getTo()))
                .map(mId -> convertToPair(mId, idEntryMap))
                .collect(Collectors.toList());
    }

    protected abstract T convertToPair(IdMappingStringPair mId, Map<String, S> idEntryMap);

    protected abstract String getEntryId(S entry);
}
