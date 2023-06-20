package org.uniprot.api.uniref.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.request.UniRefSearchRequest;
import org.uniprot.api.uniref.request.UniRefStreamRequest;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author sahmad
 * @created 13/06/2023
 */
@ExtendWith(MockitoExtension.class)
class UniRefEntryLightServiceTest {
    @Mock private UniRefQueryRepository repository;
    @Mock private UniRefFacetConfig facetConfig;
    @Mock private UniRefSortClause uniRefSortClause;
    @Mock private UniRefLightQueryResultConverter uniRefQueryResultConverter;
    @Mock private StoreStreamer<UniRefEntryLight> storeStreamer;
    @Mock private SolrQueryConfig uniRefSolrQueryConf;
    @Mock private UniProtQueryProcessorConfig uniRefQueryProcessorConfig;
    @Mock private SearchFieldConfig uniRefSearchFieldConfig;
    @Mock private RdfStreamer unirefRdfStreamer;
    @Mock private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Mock private TupleStreamDocumentIdStream solrIdStreamer;

    UniRefEntryLightService service;

    @BeforeEach
    void init() {
        service =
                new UniRefEntryLightService(
                        repository,
                        facetConfig,
                        uniRefSortClause,
                        uniRefQueryResultConverter,
                        storeStreamer,
                        uniRefSolrQueryConf,
                        uniRefQueryProcessorConfig,
                        uniRefSearchFieldConfig,
                        unirefRdfStreamer,
                        facetTupleStreamTemplate,
                        solrIdStreamer);
    }

    @Test
    void search_in_list_format_without_voldemort_being_called() {
        // when
        UniRefDocument doc1 = UniRefDocument.builder().id("UniRef100_A0A001").build();
        UniRefDocument doc2 = UniRefDocument.builder().id("UniRef100_A0A002").build();
        QueryResult<UniRefDocument> solrResult =
                QueryResult.of(Stream.of(doc1, doc2), CursorPage.of("", 10));
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery("field:value");
        searchRequest.setFormat(LIST_MEDIA_TYPE_VALUE);
        searchRequest.setSize(10);
        when(repository.searchPage(any(), any())).thenReturn(solrResult);
        // then
        QueryResult<UniRefEntryLight> result = service.search(searchRequest);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        verify(uniRefQueryResultConverter, never()).apply(any());
        assertFalse(entries.isEmpty());
        assertEquals(2, entries.size());
        List<String> ids =
                entries.stream()
                        .map(entry -> entry.getId().getValue())
                        .collect(Collectors.toList());
        assertEquals(List.of("UniRef100_A0A001", "UniRef100_A0A002"), ids);
    }

    @Test
    void search_in_non_list_format_with_voldemort_being_called() {
        // when
        UniRefDocument doc1 = UniRefDocument.builder().id("UniRef100_A0A001").build();
        UniRefEntryLight entry1 = new UniRefEntryLightBuilder().id("UniRef100_A0A001").build();
        UniRefDocument doc2 = UniRefDocument.builder().id("UniRef100_A0A002").build();
        UniRefEntryLight entry2 = new UniRefEntryLightBuilder().id("UniRef100_A0A002").build();
        QueryResult<UniRefDocument> solrResult =
                QueryResult.of(Stream.of(doc1, doc2), CursorPage.of("", 10));
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery("field:value");
        searchRequest.setFormat(APPLICATION_JSON_VALUE);
        searchRequest.setSize(10);
        when(repository.searchPage(any(), any())).thenReturn(solrResult);
        when(uniRefQueryResultConverter.apply(doc1)).thenReturn(entry1);
        when(uniRefQueryResultConverter.apply(doc2)).thenReturn(entry2);
        // then
        QueryResult<UniRefEntryLight> result = service.search(searchRequest);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        verify(uniRefQueryResultConverter, times(2)).apply(any());
        assertFalse(entries.isEmpty());
        assertEquals(2, entries.size());
        List<String> ids =
                entries.stream()
                        .map(entry -> entry.getId().getValue())
                        .collect(Collectors.toList());
        assertEquals(List.of("UniRef100_A0A001", "UniRef100_A0A002"), ids);
    }

    @Test
    void stream_in_list_format_without_voldemort_being_called() {
        UniRefStreamRequest request = new UniRefStreamRequest();
        List<String> ids =
                List.of(
                        "UniRef100_A0A001",
                        "UniRef100_A0A002",
                        "UniRef100_A0A003",
                        "UniRef100_A0A004",
                        "UniRef100_A0A005");
        request.setQuery("field:value");
        request.setFormat(LIST_MEDIA_TYPE_VALUE);
        when(solrIdStreamer.fetchIds(any())).thenReturn(ids.stream());
        Stream<UniRefEntryLight> result = service.stream(request);
        List<UniRefEntryLight> entries = result.collect(Collectors.toList());
        assertEquals(5, entries.size());
        assertEquals(
                ids, entries.stream().map(e -> e.getId().getValue()).collect(Collectors.toList()));
        verify(storeStreamer, never()).idsToStoreStream(any());
    }

    @Test
    void stream_in_non_list_format_with_voldemort_store_streamer_being_called() {
        UniRefStreamRequest request = new UniRefStreamRequest();
        List<String> ids =
                List.of(
                        "UniRef100_A0A001",
                        "UniRef100_A0A002",
                        "UniRef100_A0A003",
                        "UniRef100_A0A004",
                        "UniRef100_A0A005");
        Stream<UniRefEntryLight> entriesStream =
                ids.stream().map(id -> new UniRefEntryLightBuilder().id(id).build());
        request.setQuery("field:value");
        request.setFormat(FASTA_MEDIA_TYPE_VALUE);
        when(storeStreamer.idsToStoreStream(any())).thenReturn(entriesStream);
        Stream<UniRefEntryLight> result = service.stream(request);
        List<UniRefEntryLight> entries = result.collect(Collectors.toList());
        assertEquals(5, entries.size());
        assertEquals(
                ids, entries.stream().map(e -> e.getId().getValue()).collect(Collectors.toList()));
        verify(storeStreamer, times(1)).idsToStoreStream(any());
    }
}
