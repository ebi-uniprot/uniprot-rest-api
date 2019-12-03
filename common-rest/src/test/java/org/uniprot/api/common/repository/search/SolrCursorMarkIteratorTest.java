package org.uniprot.api.common.repository.search;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;

/**
 * Created 09/10/19
 *
 * @author Edd
 */
class SolrCursorMarkIteratorTest {
    private static final String NEXT_CURSORMARK = "first cursor mark";
    private static final List<FakeDocument> FIRST_NEXT =
            asList(new FakeDocument(), new FakeDocument(), new FakeDocument());
    private static final List<FakeDocument> SECOND_NEXT =
            asList(new FakeDocument(), new FakeDocument());
    private SolrQuery query;
    private SolrClient client;
    private SolrCollection collection;
    private Class<FakeDocument> docTypeClass;

    @BeforeEach
    void setup() {
        this.client = mock(SolrClient.class);
        this.query = mock(SolrQuery.class);
        this.collection = SolrCollection.uniprot;
        this.docTypeClass = FakeDocument.class;
    }

    @Test
    void canCreateSolrCursorarkIterator() {
        assertThat(
                new SolrResultsIterator<>(client, collection, query, docTypeClass),
                is(notNullValue()));
    }

    @Test
    void removeNotSupported() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new SolrResultsIterator<>(client, collection, query, docTypeClass).remove());
    }

    @Test
    void closeSetsVariablesToNull() {
        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);
        cursorMarkIterator.close();
        assertThat(cursorMarkIterator.getSolrClient(), is(nullValue()));
        assertThat(cursorMarkIterator.getCollection(), is(nullValue()));
        assertThat(cursorMarkIterator.getCurrentCursorMark(), is(nullValue()));
        assertThat(cursorMarkIterator.getDocumentType(), is(nullValue()));
        assertThat(cursorMarkIterator.getQuery(), is(nullValue()));
        assertThat(cursorMarkIterator.isFinished(), is(true));
    }

    @Test
    void initialCursorMarkIsSetCorrectly() {
        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);
        assertThat(
                cursorMarkIterator.getCurrentCursorMark(), is(CursorMarkParams.CURSOR_MARK_START));

        cursorMarkIterator.hasNext();
        verify(query, times(1))
                .set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
    }

    @Test
    void nullSolrResponseCausesNoNext() throws IOException, SolrServerException {
        when(client.query(collection.toString(), query)).thenReturn(null);

        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);

        assertThat(cursorMarkIterator.hasNext(), is(false));
    }

    @Test
    void validSolrResponseMeansHasNextIsTrueAndNextIsNotEmpty()
            throws IOException, SolrServerException {
        QueryResponse firstQueryResponse = firstQueryResponse();
        QueryResponse secondQueryResponse = secondQueryResponse();
        when(client.query(collection.toString(), query))
                .thenReturn(firstQueryResponse)
                .thenReturn(secondQueryResponse);

        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);

        assertThat(cursorMarkIterator.hasNext(), is(true));
        assertThat(cursorMarkIterator.next(), is(FIRST_NEXT));
        assertThat(cursorMarkIterator.hasNext(), is(true));
        assertThat(cursorMarkIterator.next(), is(SECOND_NEXT));
        assertThat(cursorMarkIterator.hasNext(), is(false));
    }

    @Test
    void canCallNextWithoutHasNext() throws IOException, SolrServerException {
        QueryResponse firstQueryResponse = firstQueryResponse();
        QueryResponse secondQueryResponse = secondQueryResponse();
        when(client.query(collection.toString(), query))
                .thenReturn(firstQueryResponse)
                .thenReturn(secondQueryResponse);

        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);

        assertThat(cursorMarkIterator.next(), is(FIRST_NEXT));
        assertThat(cursorMarkIterator.next(), is(SECOND_NEXT));
        assertThrows(NoSuchElementException.class, cursorMarkIterator::next);
    }

    @Test
    void serverErrorCausesException() throws IOException, SolrServerException {
        doThrow(SolrServerException.class).when(client).query(collection.toString(), query);

        SolrResultsIterator<FakeDocument> cursorMarkIterator =
                new SolrResultsIterator<>(client, collection, query, docTypeClass);

        assertThrows(QueryRetrievalException.class, cursorMarkIterator::next);
    }

    private QueryResponse firstQueryResponse() {
        QueryResponse mockResponse = mock(QueryResponse.class);
        when(mockResponse.getBeans(docTypeClass)).thenReturn(FIRST_NEXT);
        when(mockResponse.getNextCursorMark()).thenReturn(NEXT_CURSORMARK);
        return mockResponse;
    }

    private QueryResponse secondQueryResponse() {
        QueryResponse mockResponse = mock(QueryResponse.class);
        when(mockResponse.getBeans(docTypeClass)).thenReturn(SECOND_NEXT);
        when(mockResponse.getNextCursorMark()).thenReturn(NEXT_CURSORMARK);
        return mockResponse;
    }

    private static class FakeDocument implements Document {
        @Override
        public String getDocumentId() {
            return "anything";
        }
    }
}
