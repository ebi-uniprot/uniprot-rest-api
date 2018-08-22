package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.when;

/**
 * Created 17/08/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class TupleStreamIterableTest {
    private final static String ID = "id";

    @Mock
    private TupleStream tupleStream;

    @Test
    public void tupleStreamToStreamWithElements() throws IOException {
        when(tupleStream.read())
                .thenReturn(tuple("accession1"))  // when calling next()
                .thenReturn(tuple("accession2"))  // when calling next() 2nd time
                .thenReturn(endTuple());

        assertThat(new TupleStreamIterable(tupleStream, ID), hasItems("accession1", "accession2"));
    }

    @Test
    public void tupleStreamToIteratorWhenEmpty() throws IOException {
        when(tupleStream.read()).thenReturn(endTuple());
        assertThat(new TupleStreamIterable(tupleStream, ID), emptyIterable());
    }

    @Test
    public void canCallHasNextRepeatedlyWithoutAdvancingIterator() throws IOException {
        when(tupleStream.read())
                .thenReturn(tuple("accession1"))
                .thenReturn(tuple("accession2"))
                .thenReturn(endTuple());

        Iterator<String> tupleStreamIterator = new TupleStreamIterable(tupleStream, ID).iterator();

        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));

        assertThat(tupleStreamIterator.next(), is("accession1"));

        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));
        assertThat(tupleStreamIterator.hasNext(), is(true));

        assertThat(tupleStreamIterator.next(), is("accession2"));

        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));
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
}