package org.uniprot.api.rest.service;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;

public class BasicSearchServiceTest {
    private static BasicSearchService mockService;
    private SearchRequest request;

    @BeforeAll
    static void setUpBeforeAll() {
        mockService = Mockito.mock(BasicSearchService.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mockService.getQueryProcessor())
                .thenReturn(
                        UniProtQueryProcessor.builder()
                                .queryProcessorPipeline(
                                        new UniProtQueryNodeProcessorPipeline(emptyList()))
                                .build());
        ReflectionTestUtils.setField(
                mockService,
                "solrBatchSize",
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE); // default batch size
    }

    @BeforeEach
    void setUp() {
        request = Mockito.mock(SearchRequest.class);
    }

    @Test
    void testDownloadSolrRequestDefaults() {
        // when
        Mockito.when(request.getSize()).thenReturn(-1);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                Integer.MAX_VALUE, solrRequest.getTotalRows(), "Solr total result count mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsLessThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsEqualToBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsGreaterThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
    }

    @Test
    void testSearchSolrRequestDefaults() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE - 5);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE - 5,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE - 5,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsGreaterThanDefaultResultSize_ButLessThanBatchSize() {
        // when
        Mockito.when(request.getSize())
                .thenReturn(
                        BasicSearchService.DEFAULT_SOLR_BATCH_SIZE
                                - SearchRequest.DEFAULT_RESULTS_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - SearchRequest.DEFAULT_RESULTS_SIZE,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - SearchRequest.DEFAULT_RESULTS_SIZE,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSolrSolrRequest_SizeIsGreaterThanBatchSize() {
        // when
        Mockito.when(request.getSize())
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }
}
