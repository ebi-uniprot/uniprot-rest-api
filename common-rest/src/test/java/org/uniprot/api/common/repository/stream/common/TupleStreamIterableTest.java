package org.uniprot.api.common.repository.stream.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created 17/08/18
 *
 * @author Edd
 */
@ExtendWith(MockitoExtension.class)
class TupleStreamIterableTest {
    private static final String ID = "id";

    @Mock private TupleStream tupleStream;

    @Test
    void tupleStreamToStreamWithElements() throws IOException {
        when(tupleStream.read())
                .thenReturn(tuple("accession1")) // when calling next()
                .thenReturn(tuple("accession2")) // when calling next() 2nd time
                .thenReturn(endTuple());

        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);

        assertThat(getContents(iterable), contains("accession1", "accession2"));
        Mockito.verify(tupleStream).close();
    }

    @Test
    void tupleStreamWrappedAsStreamHasCorrectElements() throws IOException {
        when(tupleStream.read())
                .thenReturn(tuple("accession1")) // when calling next()
                .thenReturn(tuple("accession2")) // when calling next() 2nd time
                .thenReturn(endTuple());

        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);
        List<String> contents =
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());

        assertThat(contents, contains("accession1", "accession2"));
        Mockito.verify(tupleStream).close();
    }

    @Test
    void closingTupleStreamWrappedAsStreamWillCloseTupleStream() throws IOException {
        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);
        Stream<String> stream =
                StreamSupport.stream(iterable.spliterator(), false).onClose(this::closeTupleStream);

        stream.close();

        Mockito.verify(tupleStream).close();
    }

    @Test
    void closingTupleStreamWrappedAsStreamAfterOneReadWillCloseTupleStream() throws IOException {
        when(tupleStream.read())
                .thenReturn(tuple("accession1")) // when calling next()
                .thenReturn(tuple("accession2")) // when calling next() 2nd time
                .thenReturn(endTuple());

        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);
        Stream<String> stream =
                StreamSupport.stream(iterable.spliterator(), false).onClose(this::closeTupleStream);

        stream.findFirst();
        stream.close();

        Mockito.verify(tupleStream).close();
    }

    @Test
    void tupleStreamToIteratorWhenEmpty() throws IOException {
        when(tupleStream.read()).thenReturn(endTuple());
        assertThat(new TupleStreamIterable(tupleStream, ID), Matchers.emptyIterable());
        Mockito.verify(tupleStream).close();
    }

    @Test
    void retryAfterExceptionWhenReadingFromTupleStream() throws IOException {
        when(tupleStream.read())
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenReturn(tuple("accession1")) // when calling next()
                .thenReturn(endTuple());

        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);

        assertThat(getContents(iterable), contains("accession1"));
        Mockito.verify(tupleStream).close();
    }

    @Test
    void tooManyExceptionsWhenReadingCausesException() throws IOException {
        when(tupleStream.read())
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenThrow(IOException.class)
                .thenReturn(tuple("accession1")) // when calling next()
                .thenReturn(endTuple());

        TupleStreamIterable iterable = new TupleStreamIterable(tupleStream, ID);
        assertThrows(IllegalStateException.class, () -> getContents(iterable));

        Mockito.verify(tupleStream).close();
    }

    @Test
    void canCallHasNextRepeatedlyWithoutAdvancingIterator() throws IOException {
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

        assertThat(tupleStreamIterator.next(), Matchers.is("accession2"));

        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));
        assertThat(tupleStreamIterator.hasNext(), is(false));

        Mockito.verify(tupleStream).close();
    }

    private void closeTupleStream() {
        try {
            tupleStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> getContents(Iterable<String> iterable) {
        List<String> contents = new ArrayList<>();
        while (iterable.iterator().hasNext()) {
            contents.add(iterable.iterator().next());
        }
        return contents;
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
