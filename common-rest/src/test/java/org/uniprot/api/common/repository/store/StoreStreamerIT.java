package org.uniprot.api.common.repository.store;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.document.Document;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@ExtendWith(MockitoExtension.class)
class StoreStreamerIT {
    private static final int SEARCH_BATCH_SIZE = 1;
    private static final int STORE_BATCH_SIZE = 2;

    @Mock private SolrQueryRepository<FakeDocument> fakeRepository;
    @Mock private UniProtStoreClient<String> fakeStore;
    private StoreStreamer<FakeDocument, String> streamer;

    @BeforeEach
    void setUp() {
        streamer =
                StoreStreamer.<FakeDocument, String>builder()
                        .documentToId(doc -> doc.id)
                        .repository(fakeRepository)
                        .storeClient(fakeStore)
                        .searchBatchSize(SEARCH_BATCH_SIZE)
                        .storeBatchSize(STORE_BATCH_SIZE)
                        .storeFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3))
                        .build();
    }

    @Test
    void canStreamFromStore() {
        SolrRequest actualRequest =
                SolrRequest.builder().query("anything").rows(3).totalRows(3).build();
        SolrRequest modifiedRequest =
                SolrRequest.builder()
                        .query("anything")
                        .rows(SEARCH_BATCH_SIZE)
                        .totalRows(3)
                        .build();
        when(fakeRepository.getAll(modifiedRequest)).thenReturn(Stream.of(doc(1), doc(2), doc(3)));
        when(fakeStore.getEntries(anyList()))
                .thenReturn(asList("entry1", "entry2"))
                .thenReturn(singletonList("entry3"));

        List<String> results =
                streamer.idsToStoreStream(actualRequest).collect(Collectors.toList());

        assertThat(results, contains("entry1", "entry2", "entry3"));
    }

    @Test
    void canStreamIdsOnly() {
        List<FakeDocument> returnedDocs = asList(doc(1), doc(2), doc(3));
        int limit = 2;
        SolrRequest actualRequest =
                SolrRequest.builder().query("anything").rows(limit).totalRows(limit).build();
        SolrRequest modifiedRequest =
                SolrRequest.builder().query("anything").rows(limit).totalRows(limit).build();

        when(fakeRepository.getAll(modifiedRequest)).thenReturn(returnedDocs.stream());

        List<String> results = streamer.idsStream(actualRequest).collect(Collectors.toList());

        assertThat(
                results,
                is(
                        returnedDocs.stream()
                                .map(doc -> doc.id)
                                .limit(limit)
                                .collect(Collectors.toList())));
    }

    private FakeDocument doc(int id) {
        FakeDocument document = new FakeDocument();
        document.id = "id-" + id;
        return document;
    }

    private static class FakeDocument implements Document {
        public String id;

        @Override
        public String getDocumentId() {
            return "doc" + id;
        }
    }
}
