package org.uniprot.api.idmapping.service.store;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;

import lombok.*;
import net.jodah.failsafe.RetryPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.idmapping.model.*;
import org.uniprot.api.idmapping.service.store.impl.UniParcBatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.service.store.impl.UniRefBatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.RetrievalException;

class BatchStoreEntryPairIterableTest {
    private UniProtStoreClient<FakeName> storeClient;
    private RetryPolicy<Object> retryPolicy;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        storeClient = (UniProtStoreClient<FakeName>) mock(UniProtStoreClient.class);
        retryPolicy = new RetryPolicy<>().withMaxRetries(2);
    }

    @Test
    void canFetchAllBatches() {
        Iterable<IdMappingStringPair> sourceIterable =
                getIterable("from1/to1", "from2/to2", "from3/to3", "from4/to4");
        FakeBatchStoreEntryPairIterable batchStoreEntryPairIterable =
                new FakeBatchStoreEntryPairIterable(sourceIterable, 3, storeClient, retryPolicy);

        List<FakeName> storeReturnBatch1 = List.of(fn("to1", "fn1"), fn("to3", "fn3"));
        List<FakeName> storeReturnBatch2 = List.of(fn("to4", "fn4"));
        // simulate store returning *not* all of requested entries
        when(storeClient.getEntries(Set.of("to1", "to2", "to3"))).thenReturn(storeReturnBatch1);
        when(storeClient.getEntries(Set.of("to4"))).thenReturn(storeReturnBatch2);

        Iterator<Collection<FakeResultPair>> iterator = batchStoreEntryPairIterable.iterator();

        // batch 1 exists
        assertThat(iterator.hasNext(), is(true));
        assertThat(
                iterator.next(),
                contains(
                        new FakeResultPair("from1", new FakeName("to1", "fn1")),
                        new FakeResultPair("from3", new FakeName("to3", "fn3"))));

        // batch 2 exists
        assertThat(iterator.hasNext(), is(true));
        assertThat(
                iterator.next(), contains(new FakeResultPair("from4", new FakeName("to4", "fn4"))));

        // batch 3 does not exist
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    void errorInStoreCausesException() {
        Iterable<IdMappingStringPair> sourceIterable =
                getIterable("from1/to1", "from2/to2", "from3/to3");
        FakeBatchStoreEntryPairIterable batchStoreEntryPairIterable =
                new FakeBatchStoreEntryPairIterable(sourceIterable, 2, storeClient, retryPolicy);

        when(storeClient.getEntries(Set.of("to1", "to2"))).thenThrow(RetrievalException.class);

        Iterator<Collection<FakeResultPair>> iterator = batchStoreEntryPairIterable.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThrows(RetrievalException.class, iterator::next);
    }

    @Test
    void testLoggingUniParc() {
        BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry> iterable =
                new UniParcBatchStoreEntryPairIterable(List.of(), 10, null, null);
        iterable.logTiming(1, 2, 3);
        assertNotNull(iterable);
    }

    @Test
    void testLoggingUniProtKB() {
        BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry> iterable =
                new UniProtKBBatchStoreEntryPairIterable(
                        List.of(), 10, null, null, null, null, false);
        iterable.logTiming(1, 2, 3);
        assertNotNull(iterable);
    }

    @Test
    void testLoggingUniRef() {
        BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight> iterable =
                new UniRefBatchStoreEntryPairIterable(List.of(), 10, null, null);
        iterable.logTiming(1, 2, 3);
        assertNotNull(iterable);
    }

    private FakeName fn(String to, String name) {
        return FakeName.builder().to(to).name(name).build();
    }

    private Iterable<IdMappingStringPair> getIterable(String... pairs) {
        return Arrays.stream(pairs)
                .map(
                        pair -> {
                            String[] pairParts = pair.split("/");
                            return IdMappingStringPair.builder()
                                    .from(pairParts[0])
                                    .to(pairParts[1])
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    private static class FakeBatchStoreEntryPairIterable
            extends BatchStoreEntryPairIterable<FakeResultPair, FakeName> {
        protected FakeBatchStoreEntryPairIterable(
                Iterable<IdMappingStringPair> sourceIterable,
                int batchSize,
                UniProtStoreClient<FakeName> storeClient,
                RetryPolicy<Object> retryPolicy) {
            super(sourceIterable, batchSize, storeClient, retryPolicy);
        }

        @Override
        protected FakeResultPair convertToPair(
                IdMappingStringPair mId, Map<String, FakeName> idEntryMap) {
            return FakeResultPair.builder()
                    .from(mId.getFrom())
                    .to(idEntryMap.get(mId.getTo()))
                    .build();
        }

        @Override
        protected String getEntryId(FakeName entry) {
            return entry.getTo();
        }

        @Override
        protected void logTiming(int batchSize, long start, long end) {}
    }

    @AllArgsConstructor
    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    private static class FakeResultPair implements EntryPair<FakeName> {
        private final String from;
        private final FakeName to;
    }

    @AllArgsConstructor
    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    private static class FakeName {
        private final String to;
        private final String name;
    }
}
