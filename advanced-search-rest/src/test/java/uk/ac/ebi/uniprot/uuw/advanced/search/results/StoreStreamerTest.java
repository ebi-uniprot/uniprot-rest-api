package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
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
    private FakeVoldemortClient fakeVoldemortClient;
    private StoreStreamer<String> storeStreamer;

    @Before
    public void setUp() {
        fakeVoldemortClient = new FakeVoldemortClient();
    }

    private void createSearchStoreStream(int streamerBatchSize) {
        this.storeStreamer = new StoreStreamer<>(fakeVoldemortClient, streamerBatchSize, ID, DEFAULTS, s -> s);
    }

    @Test
    public void canCreateSearchStoreStream() {
        createSearchStoreStream(1);
        assertThat(storeStreamer, is(notNullValue()));
    }

    @Test
    public void canTransformSourceStreamWithUnaryBatchSize() {
        createSearchStoreStream(1);
        Stream<Collection<String>> storeStream = storeStreamer
                .idsToStoreStream(tupleStream(asList("a", "b", "c", "d", "e")));
        List<Collection<String>> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                singletonList(transformString("a")),
                singletonList(transformString("b")),
                singletonList(transformString("c")),
                singletonList(transformString("d")),
                singletonList(transformString("e"))));
    }

    @Test
    public void canTransformSourceStreamWithIntermediateBatchSize() {
        createSearchStoreStream(3);
        TupleStream tupleStream = tupleStream(asList("a", "b", "c", "d", "e"));
        Stream<Collection<String>> storeStream = storeStreamer
                .idsToStoreStream(tupleStream);
        List<Collection<String>> results = storeStream.collect(Collectors.toList());
        System.out.println(results);
        assertThat(results, contains(
                asList(transformString("a"),
                       transformString("b"),
                       transformString("c")),
                asList(transformString("d"),
                       transformString("e"))));
    }

    @Test
    public void canTransformSourceStreamWithBiggerBatchSize() {
        createSearchStoreStream(4);
        Stream<Collection<String>> storeStream = storeStreamer
                .idsToStoreStream(tupleStream(asList("a", "b", "c", "d", "e")));
        List<Collection<String>> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                asList(transformString("a"),
                       transformString("b"),
                       transformString("c"),
                       transformString("d")),
                singletonList(
                        transformString("e"))));
    }

    @Test
    public void canTransformSourceStreamWithBatchSizeGreaterThanSourceElements() {
        createSearchStoreStream(10);
        Stream<Collection<String>> storeStream = storeStreamer
                .idsToStoreStream(tupleStream(asList("a", "b", "c", "d", "e")));
        List<Collection<String>> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                asList(transformString("a"),
                       transformString("b"),
                       transformString("c"),
                       transformString("d"),
                       transformString("e"))));
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

    public static String transformString(String id) {
        return id + "-transformed";
    }
}