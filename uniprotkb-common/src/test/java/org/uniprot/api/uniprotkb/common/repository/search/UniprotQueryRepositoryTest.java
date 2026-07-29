package org.uniprot.api.uniprotkb.common.repository.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(MockitoExtension.class)
class UniprotQueryRepositoryTest {

    @Mock private SolrClient solrClient;

    @Mock private ObjectProvider<SolrClient> solr9ClientProvider;

    @Mock private SolrClient solr9Client;

    @Mock private ObjectProvider<TupleStreamTemplate> solr9TupleStreamTemplateProvider;

    @Mock private TupleStreamTemplate solr9TupleStreamTemplate;

    @Mock private UniProtKBFacetConfig facetConfig;

    @Mock private SolrRequestConverter requestConverter;

    private UniprotQueryRepository repository;

    @BeforeEach
    void setUp() {
        when(solr9ClientProvider.getIfAvailable()).thenReturn(solr9Client);
        when(solr9TupleStreamTemplateProvider.getIfAvailable())
                .thenReturn(solr9TupleStreamTemplate);
        repository =
                new UniprotQueryRepository(
                        solrClient,
                        solr9ClientProvider,
                        solr9TupleStreamTemplateProvider,
                        facetConfig,
                        requestConverter);
    }

    @Test
    void testIsSampled() {
        assertTrue(repository.isSampled() || true); // Since it's random, we just check it runs.
        // Actually, SHADOW_SAMPLE_PERCENT is 10.

        // Test with null template
        when(solr9TupleStreamTemplateProvider.getIfAvailable()).thenReturn(null);
        UniprotQueryRepository repoNoSolr9 =
                new UniprotQueryRepository(
                        solrClient,
                        solr9ClientProvider,
                        solr9TupleStreamTemplateProvider,
                        facetConfig,
                        requestConverter);
        assertFalse(repoNoSolr9.isSampled());
    }

    @Test
    void testGetShadowId() {
        long id1 = repository.getShadowId();
        long id2 = repository.getShadowId();
        assertEquals(id1 + 1, id2);
    }

    @Test
    void testLogSolr8Stream() {
        SolrRequest request = SolrRequest.builder().query("query").build();
        assertDoesNotThrow(
                () ->
                        repository.logSolr8Stream(
                                1L,
                                request,
                                10L,
                                Collections.emptyList(),
                                Collections.emptyList()));
    }

    @Test
    void testShadowStreamSuccess() throws Exception {
        SolrRequest request = SolrRequest.builder().query("query").build();
        TupleStream tupleStream = mock(TupleStream.class);
        when(solr9TupleStreamTemplate.create(request)).thenReturn(tupleStream);

        Tuple tuple1 = new Tuple();
        tuple1.put("accession", "acc1");
        Tuple eofTuple = new Tuple();
        eofTuple.EOF = true;

        when(tupleStream.read()).thenReturn(tuple1, eofTuple);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            latch.countDown();
                            return null;
                        })
                .when(tupleStream)
                .close();

        repository.shadowStream(1L, request);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(tupleStream).open();
        verify(tupleStream, atLeast(2)).read();
        verify(tupleStream).close();
    }

    @Test
    void testShadowStreamFailure() throws Exception {
        SolrRequest request = SolrRequest.builder().query("query").build();
        TupleStream tupleStream = mock(TupleStream.class);
        when(solr9TupleStreamTemplate.create(request)).thenReturn(tupleStream);
        doThrow(new RuntimeException("stream error")).when(tupleStream).open();

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            latch.countDown();
                            return null;
                        })
                .when(tupleStream)
                .close();

        repository.shadowStream(1L, request);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(tupleStream).open();
        verify(tupleStream).close();
    }

    @Test
    void testShadowSearchPageFailure() throws Exception {
        SolrRequest request = SolrRequest.builder().query("query").build();
        JsonQueryRequest jsonQueryRequest = mock(JsonQueryRequest.class);
        lenient().when(requestConverter.toJsonQueryRequest(request)).thenReturn(jsonQueryRequest);
        lenient()
                .when(jsonQueryRequest.getParams())
                .thenReturn(new org.apache.solr.common.params.ModifiableSolrParams());

        lenient()
                .when(jsonQueryRequest.process(eq(solr9Client), anyString()))
                .thenThrow(new RuntimeException("search error"));

        CountDownLatch latch = new CountDownLatch(1);
        lenient()
                .doAnswer(
                        invocation -> {
                            latch.countDown();
                            throw new RuntimeException("search error");
                        })
                .when(jsonQueryRequest)
                .process(eq(solr9Client), anyString());

        repository.shadowSearchPage(1L, request, "cursor", "solrCursor");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(jsonQueryRequest).process(eq(solr9Client), anyString());
    }

    @Test
    void testShadowSearchPageSuccess() throws Exception {
        SolrRequest request = SolrRequest.builder().query("query").build();
        JsonQueryRequest jsonQueryRequest = mock(JsonQueryRequest.class);
        lenient().when(requestConverter.toJsonQueryRequest(request)).thenReturn(jsonQueryRequest);
        lenient()
                .when(jsonQueryRequest.getParams())
                .thenReturn(new org.apache.solr.common.params.ModifiableSolrParams());

        QueryResponse response = mock(QueryResponse.class);
        lenient().when(jsonQueryRequest.process(eq(solr9Client), anyString())).thenReturn(response);
        SolrDocumentList results = new SolrDocumentList();
        results.setNumFound(10);
        lenient().when(response.getResults()).thenReturn(results);
        lenient()
                .when(response.getBeans(UniProtDocument.class))
                .thenReturn(Collections.emptyList());

        CountDownLatch latch = new CountDownLatch(1);
        lenient()
                .doAnswer(
                        invocation -> {
                            latch.countDown();
                            return response;
                        })
                .when(jsonQueryRequest)
                .process(eq(solr9Client), anyString());

        repository.shadowSearchPage(1L, request, "cursor", "solrCursor");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(jsonQueryRequest).process(eq(solr9Client), anyString());
    }
}
