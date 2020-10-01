package org.uniprot.api.rest.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.Data;

import org.junit.jupiter.api.BeforeAll;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.search.FakeSolrSortClause;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.Document;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicSearchServiceTest {
    private BasicSearchService<FakeDocument, FakeEntity> service;
    @Mock private SolrQueryRepository<FakeDocument> repository;
    @Mock private Function<FakeDocument, FakeEntity> entryConverter;
    @Mock private AbstractSolrSortClause solrSortClause;
    @Mock private SolrQueryConfig queryBoosts;
    @Mock private FacetConfig facetConfig;
    @Mock private SearchRequest request;

    private static final int defaultPageSize = 10;

    @BeforeEach
    void setUpBeforeAll() {
        service =
                new BasicSearchService<FakeDocument, FakeEntity>(
                        repository, entryConverter, solrSortClause, queryBoosts, facetConfig) {
                    @Override
                    protected SearchFieldItem getIdField() {
                        SearchFieldItem item = new SearchFieldItem();
                        item.setFieldName("id");
                        return item;
                    }

                    @Override
                    protected QueryProcessor getQueryProcessor() {
                        return new UniProtQueryProcessor(emptyList(), emptyMap());
                    }
                };
        ReflectionTestUtils.setField(
                service,
                "solrBatchSize",
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE); // default batch size

        ReflectionTestUtils.setField(
                service, "defaultPageSize", defaultPageSize);
    }

    @Test
    void getEntity_searchThatFindsNothingCausesResourceNotFoundException() {
        // when and then
        when(repository.getEntry(any(SolrRequest.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEntity("id", "field"));
    }

    @Test
    void getEntity_searchExceptionCausesServiceException() {
        // when and then
        when(repository.getEntry(any(SolrRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(ServiceException.class, () -> service.getEntity("id", "field"));
    }

    @Test
    void getEntity_conversionExceptionCausesServiceException() {
        // when and then
        when(repository.getEntry(any(SolrRequest.class)))
                .thenReturn(Optional.of(FakeDocument.builder().id("1").build()));
        when(entryConverter.apply(any(FakeDocument.class))).thenThrow(RuntimeException.class);

        assertThrows(ServiceException.class, () -> service.getEntity("id", "field"));
    }

    @Test
    void getEntity_conversionReturningNullCausesServiceException() {
        // when and then
        when(repository.getEntry(any(SolrRequest.class)))
                .thenReturn(Optional.of(FakeDocument.builder().id("1").build()));
        when(entryConverter.apply(any(FakeDocument.class))).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.getEntity("id", "field"));
    }

    @Test
    void getEntity_conversionReturningNonNullSucceeds() {
        // when and then
        when(repository.getEntry(any(SolrRequest.class)))
                .thenReturn(Optional.of(FakeDocument.builder().id("1").build()));
        when(entryConverter.apply(any(FakeDocument.class)))
                .thenReturn(FakeEntity.builder().id("1").build());

        FakeEntity entity = service.getEntity("id", "field");

        assertEquals("1", entity.id);
    }

    @Test
    void testDownloadSolrRequestDefaults() {
        // when
        TestSearchRequest request = new TestSearchRequest();
        request.setQuery("queryValue");
        SolrRequest solrRequest = service.createDownloadSolrRequest(request);
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

        SolrRequest solrRequest = service.createDownloadSolrRequest(request);
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

        SolrRequest solrRequest = service.createDownloadSolrRequest(request);
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

        SolrRequest solrRequest = service.createDownloadSolrRequest(request);
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
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
    @Builder
    private static class FakeDocument implements Document {
        String id;

        @Override
        public String getDocumentId() {
            return id;
        }
    }

    @Builder
    private static class FakeEntity {
        String id;
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
