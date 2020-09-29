package org.uniprot.api.rest.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;

@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class BasicSearchServiceTest {
    private static BasicSearchService mockService;
    private SearchRequest request;

    private static final int defaultPageSize = 10;

    @BeforeAll
    void setUpBeforeAll() {
        mockService = Mockito.mock(BasicSearchService.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mockService.getQueryProcessor())
                .thenReturn(new UniProtQueryProcessor(emptyList(), emptyMap()));
        ReflectionTestUtils.setField(
                mockService,
                "solrBatchSize",
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE); // default batch size
        ReflectionTestUtils.setField(
                mockService, "defaultPageSize", defaultPageSize); // default batch size
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
        Mockito.when(request.getSize()).thenReturn(defaultPageSize);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                defaultPageSize,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                defaultPageSize,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(defaultPageSize - 5);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(defaultPageSize - 5, solrRequest.getRows(), "Solr batch size mismatch");
        assertEquals(
                defaultPageSize - 5,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(defaultPageSize);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                defaultPageSize,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        assertEquals(
                defaultPageSize,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsGreaterThanDefaultResultSize_ButLessThanBatchSize() {
        // when
        Mockito.when(request.getSize())
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - defaultPageSize);
        Mockito.when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - defaultPageSize,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - defaultPageSize,
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
        int size = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2;
        Mockito.when(request.getSize()).thenReturn(size);
        Mockito.when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(size, Integer.valueOf(solrRequest.getRows()), "Solr batch size mismatch");
        assertEquals(
                size,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }
}
