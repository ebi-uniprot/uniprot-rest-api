package org.uniprot.api;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;

public class BasicSearchServiceTest {
    private static BasicSearchService mockService;
    private SearchRequest request;

    @BeforeAll
    static void setUpBeforeAll() {
        mockService = Mockito.mock(BasicSearchService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(
                mockService,
                "solrBatchSize",
                Optional.of(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)); // default batch size
    }

    @BeforeEach
    void setUp() {
        request = Mockito.mock(SearchRequest.class);
    }

    @Test
    void testDownloadSolrRequestDefaults() {
        // when
        Mockito.when(request.getSize()).thenReturn(-1);
        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        // then
        Assertions.assertNotNull(solrRequest);
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                Integer.MAX_VALUE, solrRequest.getTotalRows(), "Solr total result count mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsLessThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsEqualToBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testDownloadSolrRequest_SizeIsGreaterThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2);

        SolrRequest solrRequest = mockService.createDownloadSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
    }

    @Test
    void testSearchSolrRequestDefaults() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        Assertions.assertNotNull(solrRequest);
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE - 5);
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        Assertions.assertNotNull(solrRequest);
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE - 5,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE - 5,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToDefaultResultSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        Assertions.assertNotNull(solrRequest);
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                SearchRequest.DEFAULT_RESULTS_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsGreaterThanDefaultResultSize_ButLessThanBatchSize() {
        // when
        Mockito.when(request.getSize())
                .thenReturn(
                        BasicSearchService.DEFAULT_SOLR_BATCH_SIZE
                                - SearchRequest.DEFAULT_RESULTS_SIZE);
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        Assertions.assertNotNull(solrRequest);
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - SearchRequest.DEFAULT_RESULTS_SIZE,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - SearchRequest.DEFAULT_RESULTS_SIZE,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getRows(),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2,
                solrRequest.getTotalRows(),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToBatchSize() {
        // when
        Mockito.when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSolrSolrRequest_SizeIsGreaterThanBatchSize() {
        // when
        Mockito.when(request.getSize())
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        Assertions.assertNotNull(solrRequest);
        // then
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getRows()),
                "Solr batch size mismatch");
        Assertions.assertEquals(
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE,
                Integer.valueOf(solrRequest.getTotalRows()),
                "Solr total result count mismatch");
        Assertions.assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }
}
