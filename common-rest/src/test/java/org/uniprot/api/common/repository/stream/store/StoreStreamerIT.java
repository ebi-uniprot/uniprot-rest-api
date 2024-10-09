package org.uniprot.api.common.repository.stream.store;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;
import static org.uniprot.api.common.repository.stream.store.TupleStreamUtils.tupleStream;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.DocumentIdStream;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.RetrievalException;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.search.document.Document;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class StoreStreamerIT {
    private static final String ID = "id";
    private static final int STORE_BATCH_SIZE = 2;
    private static final String FAKE_QUERY = "any query";
    private static final String FAKE_FILTER_QUERY = "any filter query";

    private SolrRequest solrRequest;
    @Mock private UniProtStoreClient<String> fakeStore;
    @Mock private VoldemortClient<String> fakeClient;

    @BeforeEach
    void setUp() {
        fakeStore = new FakeUniProtStoreClient(fakeClient);
        solrRequest =
                SolrRequest.builder().query(FAKE_QUERY).filterQuery(FAKE_FILTER_QUERY).build();
    }

    @Test
    void whenStoreExceptionDuringIdStreaming_thenEnsureExceptionThrown() {
        List<String> ids = asList("a", "b", "c", "d", "e");
        when(fakeClient.getEntries(anyIterable())).thenThrow(RetrievalException.class);
        StoreStreamer<String> storeStreamer = createSearchStoreStream(1, tupleStream(ids));

        assertThrows(
                RetrievalException.class,
                () ->
                        storeStreamer
                                .idsToStoreStream(solrRequest, StoreRequest.builder().build())
                                .collect(Collectors.toList()));
    }

    @Test
    void canIdStreamWithUnaryBatchSize() {
        StoreStreamer<String> storeStreamer =
                createSearchStoreStream(1, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer.idsStream(solrRequest);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains("a", "b", "c", "d", "e"));
    }

    @Test
    void canTransformSourceStreamWithUnaryBatchSize() {
        StoreStreamer<String> storeStreamer =
                createSearchStoreStream(1, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream =
                storeStreamer.idsToStoreStream(solrRequest, StoreRequest.builder().build());
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(
                results,
                contains(
                        transformString("a"),
                        transformString("b"),
                        transformString("c"),
                        transformString("d"),
                        transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithIntermediateBatchSize() {
        StoreStreamer<String> storeStreamer =
                createSearchStoreStream(3, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream =
                storeStreamer.idsToStoreStream(solrRequest, StoreRequest.builder().build());
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(
                results,
                contains(
                        transformString("a"),
                        transformString("b"),
                        transformString("c"),
                        transformString("d"),
                        transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithBiggerBatchSize() {
        StoreStreamer<String> storeStreamer =
                createSearchStoreStream(4, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream =
                storeStreamer.idsToStoreStream(solrRequest, StoreRequest.builder().build());
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(
                results,
                contains(
                        transformString("a"),
                        transformString("b"),
                        transformString("c"),
                        transformString("d"),
                        transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithBatchSizeGreaterThanSourceElements() {
        StoreStreamer<String> storeStreamer =
                createSearchStoreStream(10, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream =
                storeStreamer.idsToStoreStream(solrRequest, StoreRequest.builder().build());
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(
                results,
                contains(
                        transformString("a"),
                        transformString("b"),
                        transformString("c"),
                        transformString("d"),
                        transformString("e")));
    }

    @Test
    void whenStoreExceptionDuringIdStreaming_thenEnsureIOExceptionThrown() throws IOException {
        List<String> ids = asList("a", "b", "c", "d", "e");
        TupleStream tupleStream = tupleStream(ids);
        Mockito.doThrow(new IOException()).when(tupleStream).open();
        StoreStreamer<String> storeStreamer = createSearchStoreStream(1, tupleStream);

        assertThrows(
                IllegalStateException.class,
                () ->
                        storeStreamer
                                .idsToStoreStream(solrRequest, StoreRequest.builder().build())
                                .collect(Collectors.toList()));
    }

    @Test
    void whenIOExceptionDuringIdsStream() throws IOException {
        TupleStream tupleStream = tupleStream(asList("a", "b", "c", "d", "e"));
        Mockito.doThrow(new IOException()).when(tupleStream).open();
        StoreStreamer<String> storeStreamer = createSearchStoreStream(1, tupleStream);
        assertThrows(IllegalStateException.class, () -> storeStreamer.idsStream(solrRequest));
    }

    private StoreStreamer<String> createSearchStoreStream(
            int streamerBatchSize, TupleStream tupleStream) {
        TupleStreamTemplate mockTupleStreamTemplate = Mockito.mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(ArgumentMatchers.any())).thenReturn(tupleStream);

        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setIdFieldName(ID);
        streamConfig.setStoreBatchSize(STORE_BATCH_SIZE);
        DocumentIdStream idStream =
                TupleStreamDocumentIdStream.builder()
                        .tupleStreamTemplate(mockTupleStreamTemplate)
                        .streamConfig(streamConfig)
                        .build();

        StoreStreamerConfig<String> storeStreamerConfig =
                StoreStreamerConfig.<String>builder()
                        .streamConfig(streamConfig)
                        .tupleStreamTemplate(mockTupleStreamTemplate)
                        .storeClient(fakeStore)
                        .storeFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3))
                        .documentIdStream(idStream)
                        .build();
        return new StoreStreamer<>(storeStreamerConfig);
    }

    private FakeDocument doc(int id) {
        FakeDocument document = new FakeDocument();
        document.id = "id-" + id;
        return document;
    }

    static String transformString(String id) {
        return id + "-transformed";
    }

    private static class FakeDocument implements Document {
        private static final long serialVersionUID = 7689868007129327083L;
        public String id;

        @Override
        public String getDocumentId() {
            return "doc" + id;
        }
    }

    private static class FakeUniProtStoreClient extends UniProtStoreClient<String> {
        private final VoldemortClient<String> client;

        FakeUniProtStoreClient(VoldemortClient<String> client) {
            super(client);
            this.client = client;
        }

        @Override
        public String getStoreName() {
            return null;
        }

        @Override
        public Optional<String> getEntry(String s) {
            return Optional.empty();
        }

        @Override
        public List<String> getEntries(Iterable<String> iterable) {
            // this simulates that the store client is used to retrieve data
            // this has NO impact on the list returned from this method
            client.getEntries(iterable);

            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(StoreStreamerIT::transformString)
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getEntryMap(Iterable<String> iterable) {
            return null;
        }

        @Override
        public void saveEntry(String s) {}
    }
}
