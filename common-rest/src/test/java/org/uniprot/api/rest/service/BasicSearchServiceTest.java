package org.uniprot.api.rest.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.Data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.FakeSolrSortClause;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;

@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class BasicSearchServiceTest {
    private static BasicSearchService mockService;

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
        ReflectionTestUtils.setField(mockService, "facetConfig", new FakeFacetConfig());

        ReflectionTestUtils.setField(mockService, "solrSortClause", new FakeSolrSortClause());
    }

    @Test
    void testDownloadSolrRequestDefaults() {
        // when
        TestSearchRequest request = new TestSearchRequest();
        request.setQuery("queryValue");
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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);
        request.setQuery("queryValue");

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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        request.setQuery("queryValue");

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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2);
        request.setQuery("queryValue");

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
        TestSearchRequest request = new TestSearchRequest();
        request.setQuery("queryValue");
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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(defaultPageSize - 5);
        request.setQuery("queryValue");
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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(defaultPageSize);
        request.setQuery("queryValue");
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
        int size = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - defaultPageSize;
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(size);
        request.setQuery("queryValue");
        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        // then
        assertNotNull(solrRequest);
        assertEquals(size, solrRequest.getRows(), "Solr batch size mismatch");
        assertEquals(size, solrRequest.getTotalRows(), "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsLessThanBatchSize() {
        // when
        int size = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2;
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(size);
        request.setQuery("queryValue");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        // then
        assertEquals(size, solrRequest.getRows(), "Solr batch size mismatch");
        assertEquals(size, solrRequest.getTotalRows(), "Solr total result count mismatch");
        assertEquals(
                solrRequest.getRows(), solrRequest.getTotalRows(), "rows and totalRows mismatch");
    }

    @Test
    void testSearchSolrRequest_SizeIsEqualToBatchSize() {
        // when
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        request.setQuery("queryValue");

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
        TestSearchRequest request = new TestSearchRequest();
        request.setSize(size);
        request.setQuery("queryValue");

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

    @Test
    void testSearchSolrRequest_WithFacets() {
        // when
        TestSearchRequest request = new TestSearchRequest();
        request.setQuery("queryValue");
        request.setFacets("facet1,facet2");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);

        // then
        assertNotNull(solrRequest.getFacets());
        assertEquals(2, solrRequest.getFacets().size());
        assertEquals("facet1", solrRequest.getFacets().get(0));
        assertEquals("facet2", solrRequest.getFacets().get(1));

        assertNotNull(solrRequest.getFacetConfig());
    }

    @Test
    void testSearchSolrRequest_WithSort() {
        // when
        TestSearchRequest request = new TestSearchRequest();
        request.setQuery("queryValue");
        request.setSort("field1 asc");

        SolrRequest solrRequest = mockService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);

        // then
        assertNotNull(solrRequest.getSorts());
        assertEquals(2, solrRequest.getSorts().size());
    }

    @Data
    private static class TestSearchRequest implements SearchRequest {

        private Integer size;
        private String cursor;
        private String facets;
        private String query;
        private String fields;
        private String sort;
    }
}
