package org.uniprot.api.rest.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import lombok.Builder;
import lombok.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.uniprot.api.rest.search.FakeSolrSortClause;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.Document;

@ExtendWith(MockitoExtension.class)
class BasicSearchServiceTest {
    private BasicSearchService<FakeDocument, FakeEntity> service;
    private final AbstractSolrSortClause solrSortClause = new FakeSolrSortClause();
    private final FacetConfig facetConfig = new FakeFacetConfig();
    @Mock private SolrQueryRepository<FakeDocument> repository;
    @Mock private Function<FakeDocument, FakeEntity> entryConverter;
    @Mock private SolrQueryConfig queryBoosts;

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

        ReflectionTestUtils.setField(service, "defaultPageSize", defaultPageSize);
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
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
        FakeSearchRequest request = new FakeSearchRequest();
        request.setQuery("queryValue");
        request.setFacets("facet1,facet2");

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        FakeSearchRequest request = new FakeSearchRequest();
        request.setQuery("queryValue");
        request.setSort("field1 asc");

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
        assertNotNull(solrRequest);

        // then
        assertNotNull(solrRequest.getSorts());
        assertEquals(2, solrRequest.getSorts().size());
    }

    @Builder
    private static class FakeDocument implements Document {

        private static final long serialVersionUID = -2078841376204509749L;

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
    private static class FakeSearchRequest implements SearchRequest {

        private Integer size;
        private String cursor;
        private String facets;
        private String query;
        private String fields;
        private String sort;
    }
}
