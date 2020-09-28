package org.uniprot.api.rest.service;

import lombok.Builder;
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
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.Document;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BasicSearchServiceTest {
    private BasicSearchService<FakeDocument, FakeEntity> service;
    @Mock private SolrQueryRepository<FakeDocument> repository;
    @Mock private Function<FakeDocument, FakeEntity> entryConverter;
    @Mock private AbstractSolrSortClause solrSortClause;
    @Mock private SolrQueryConfig queryBoosts;
    @Mock private FacetConfig facetConfig;
    @Mock private SearchRequest request;

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
                        return new UniProtQueryProcessor(emptyList());
                    }
                };
        ReflectionTestUtils.setField(
                service,
                "solrBatchSize",
                BasicSearchService.DEFAULT_SOLR_BATCH_SIZE); // default batch size
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
        when(request.getSize()).thenReturn(-1);
        when(request.getQuery()).thenReturn("queryValue");
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
        when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);
        when(request.getQuery()).thenReturn("queryValue");

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

        when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        when(request.getQuery()).thenReturn("queryValue");

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
        when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2);
        when(request.getQuery()).thenReturn("queryValue");

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
        when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE - 5);
        when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        when(request.getSize()).thenReturn(SearchRequest.DEFAULT_RESULTS_SIZE);
        when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        when(request.getSize())
                .thenReturn(
                        BasicSearchService.DEFAULT_SOLR_BATCH_SIZE
                                - SearchRequest.DEFAULT_RESULTS_SIZE);
        when(request.getQuery()).thenReturn("queryValue");
        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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
        when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 2);
        when(request.getQuery()).thenReturn("queryValue");

        SolrRequest solrRequest = service.createSearchSolrRequest(request);
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

        when(request.getSize()).thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        when(request.getQuery()).thenReturn("queryValue");

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
        when(request.getSize())
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 2)
                .thenReturn(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE);
        when(request.getQuery()).thenReturn("queryValue");

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
}
