package org.uniprot.api.rest.service;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.Document;

import lombok.Builder;

@ExtendWith(MockitoExtension.class)
class BasicSearchServiceTest {
    private static final int DEFAULT_SOLR_BATCH_SIZE = 100;
    private BasicSearchService<FakeDocument, FakeEntity> service;
    @Mock private RequestConverter requestConverter;
    @Mock private SolrQueryRepository<FakeDocument> repository;
    @Mock private Function<FakeDocument, FakeEntity> entryConverter;

    @BeforeEach
    void setUpBeforeAll() {
        service =
                new BasicSearchService<FakeDocument, FakeEntity>(
                        repository, entryConverter, requestConverter) {
                    @Override
                    protected SearchFieldItem getIdField() {
                        SearchFieldItem item = new SearchFieldItem();
                        item.setFieldName("id");
                        return item;
                    }
                };
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
}
