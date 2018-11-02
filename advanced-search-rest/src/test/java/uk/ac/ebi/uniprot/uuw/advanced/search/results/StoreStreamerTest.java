package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class StoreStreamerTest {
    private static final String ID = "id";
    private static final String DEFAULTS = "defaults";
    private static final String FAKE_QUERY = "any query";
    private static final String FAKE_FILTER_QUERY = "any filter query";
    private static final Sort FAKE_SORT = new Sort(Sort.Direction.ASC, "any field");
    private FakeVoldemortClient fakeVoldemortClient;
    private StoreStreamer<String> storeStreamer;

    public static String transformString(String id) {
        return id + "-transformed";
    }

    @Before
    public void setUp() {
        fakeVoldemortClient = new FakeVoldemortClient();
    }

    @Test
    public void canCreateSearchStoreStream() {
        createSearchStoreStream(1, tupleStream(singletonList("a")));
        assertThat(storeStreamer, is(notNullValue()));
    }

    @Test
    public void canTransformSourceStreamWithUnaryBatchSize() {
        createSearchStoreStream(1, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer
                .idsToStoreStream(FAKE_QUERY, FAKE_FILTER_QUERY, FAKE_SORT);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    public void canTransformSourceStreamWithIntermediateBatchSize() {
        createSearchStoreStream(3, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer
                .idsToStoreStream(FAKE_QUERY, FAKE_FILTER_QUERY, FAKE_SORT);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    public void canTransformSourceStreamWithBiggerBatchSize() {
        createSearchStoreStream(4, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer
                .idsToStoreStream(FAKE_QUERY, FAKE_FILTER_QUERY, FAKE_SORT);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    public void canTransformSourceStreamWithBatchSizeGreaterThanSourceElements() {
        createSearchStoreStream(10, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer
                .idsToStoreStream(FAKE_QUERY, FAKE_FILTER_QUERY, FAKE_SORT);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    private void createSearchStoreStream(int streamerBatchSize, TupleStream tupleStream) {
        TupleStreamTemplate mockTupleStreamTemplate = mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(anyString(),anyString(), anyString(), ArgumentMatchers.any())).thenReturn(tupleStream);
        this.storeStreamer = StoreStreamer.<String>builder()
                .storeClient(fakeVoldemortClient)
                .streamerBatchSize(streamerBatchSize)
                .id(ID)
                .tupleStreamTemplate(mockTupleStreamTemplate)
                .defaultsField(DEFAULTS)
                .defaultsConverter(s -> s)
                .build();
    }

    private TupleStream tupleStream(Collection<String> values) {
        TupleStream mockTupleStream = mock(TupleStream.class);

        try {
            OngoingStubbing<Tuple> ongoingStubbing = when(mockTupleStream.read());
            for (String value : values) {
                System.out.println("hello " + value);
                ongoingStubbing = ongoingStubbing.thenReturn(tuple(value));
            }

            ongoingStubbing = ongoingStubbing.thenReturn(endTuple());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mockTupleStream;
    }

    private Tuple tuple(String accession) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put(ID, accession);
        return new Tuple(valueMap);
    }

    private Tuple endTuple() {
        Map<String, String> eofMap = new HashMap<>();
        eofMap.put("EOF", "");
        return new Tuple(eofMap);
    }

    private static class FakeVoldemortClient implements VoldemortClient<String> {
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
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(StoreStreamerTest::transformString)
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getEntryMap(Iterable<String> iterable) {
            return null;
        }

        @Override
        public void saveEntry(String s) {

        }
    }
}