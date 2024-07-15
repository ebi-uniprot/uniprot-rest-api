package org.uniprot.api.uniparc.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.response.converter.UniParcQueryResultConverter;
import org.uniprot.api.uniparc.common.service.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.api.uniparc.common.service.sort.UniParcSortClause;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author sahmad
 * @created 13/06/2023
 */
@ExtendWith(MockitoExtension.class)
class UniParcQueryServiceTest {
    @Mock private UniParcQueryRepository repository;
    @Mock private UniParcFacetConfig facetConfig;
    @Mock private UniParcSortClause solrSortClause;
    @Mock private UniParcQueryResultConverter uniParcQueryResultConverter;
    @Mock private StoreStreamer<UniParcEntry> storeStreamer;
    @Mock private SolrQueryConfig uniParcSolrQueryConf;
    @Mock private UniProtQueryProcessorConfig uniParcQueryProcessorConfig;
    @Mock private SearchFieldConfig uniParcSearchFieldConfig;
    @Mock private RdfStreamer uniparcRdfStreamer;
    @Mock private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Mock private TupleStreamDocumentIdStream solrIdStreamer;
    private UniParcQueryService service;

    @BeforeEach
    void init() {
        service =
                new UniParcQueryService(
                        repository,
                        facetConfig,
                        solrSortClause,
                        uniParcQueryResultConverter,
                        storeStreamer,
                        uniParcSolrQueryConf,
                        uniParcQueryProcessorConfig,
                        uniParcSearchFieldConfig,
                        uniparcRdfStreamer,
                        facetTupleStreamTemplate,
                        solrIdStreamer);
    }

    @Test
    void search_in_list_format_without_voldemort_being_called() {
        // when
        UniParcDocument doc1 =
                new UniParcDocument.UniParcDocumentBuilder().upi("UPI0000000001").build();
        UniParcDocument doc2 =
                new UniParcDocument.UniParcDocumentBuilder().upi("UPI0000000002").build();
        QueryResult<UniParcDocument> solrResult =
                QueryResult.<UniParcDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(CursorPage.of("", 10))
                        .build();
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery("field:value");
        searchRequest.setFormat(UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
        searchRequest.setSize(10);
        when(repository.searchPage(any(), any())).thenReturn(solrResult);
        // then
        QueryResult<UniParcEntry> result = service.search(searchRequest);
        List<UniParcEntry> entries = result.getContent().collect(Collectors.toList());
        verify(uniParcQueryResultConverter, never()).apply(any());
        assertFalse(entries.isEmpty());
        assertEquals(2, entries.size());
        List<String> upis =
                entries.stream()
                        .map(entry -> entry.getUniParcId().getValue())
                        .collect(Collectors.toList());
        assertEquals(List.of("UPI0000000001", "UPI0000000002"), upis);
    }

    @Test
    void search_in_non_list_format_with_voldemort_being_called() {
        // when
        UniParcDocument doc1 =
                new UniParcDocument.UniParcDocumentBuilder().upi("UPI0000000001").build();
        UniParcEntry entry1 = new UniParcEntryBuilder().uniParcId("UPI0000000001").build();
        UniParcDocument doc2 =
                new UniParcDocument.UniParcDocumentBuilder().upi("UPI0000000002").build();
        UniParcEntry entry2 = new UniParcEntryBuilder().uniParcId("UPI0000000002").build();
        QueryResult<UniParcDocument> solrResult =
                QueryResult.<UniParcDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(CursorPage.of("", 10))
                        .build();
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery("field:value");
        searchRequest.setFormat(MediaType.APPLICATION_JSON_VALUE);
        searchRequest.setSize(10);
        when(repository.searchPage(any(), any())).thenReturn(solrResult);
        when(uniParcQueryResultConverter.apply(doc1)).thenReturn(entry1);
        when(uniParcQueryResultConverter.apply(doc2)).thenReturn(entry2);
        // then
        QueryResult<UniParcEntry> result = service.search(searchRequest);
        List<UniParcEntry> entries = result.getContent().collect(Collectors.toList());
        verify(uniParcQueryResultConverter, times(2)).apply(any());
        assertFalse(entries.isEmpty());
        assertEquals(2, entries.size());
        List<String> upis =
                entries.stream()
                        .map(entry -> entry.getUniParcId().getValue())
                        .collect(Collectors.toList());
        assertEquals(List.of("UPI0000000001", "UPI0000000002"), upis);
    }

    @Test
    void stream_in_list_format_without_voldemort_being_called() {
        UniParcStreamRequest request = new UniParcStreamRequest();
        List<String> upis =
                List.of(
                        "UPI0000000001",
                        "UPI0000000002",
                        "UPI0000000003",
                        "UPI0000000004",
                        "UPI0000000005");
        request.setQuery("field:value");
        request.setFormat(UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
        when(solrIdStreamer.fetchIds(any())).thenReturn(upis.stream());
        Stream<UniParcEntry> result = service.stream(request);
        List<UniParcEntry> entries = result.collect(Collectors.toList());
        assertEquals(5, entries.size());
        assertEquals(
                upis,
                entries.stream()
                        .map(e -> e.getUniParcId().getValue())
                        .collect(Collectors.toList()));
        verify(storeStreamer, never()).idsToStoreStream(any(), any());
    }

    @Test
    void stream_in_non_list_format_with_voldemort_store_streamer_being_called() {
        UniParcStreamRequest request = new UniParcStreamRequest();
        List<String> upis =
                List.of(
                        "UPI0000000001",
                        "UPI0000000002",
                        "UPI0000000003",
                        "UPI0000000004",
                        "UPI0000000005");
        Stream<UniParcEntry> entriesStream =
                upis.stream().map(id -> new UniParcEntryBuilder().uniParcId(id).build());
        request.setQuery("field:value");
        request.setFormat(UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        when(storeStreamer.idsToStoreStream(any(), any())).thenReturn(entriesStream);
        Stream<UniParcEntry> result = service.stream(request);
        List<UniParcEntry> entries = result.collect(Collectors.toList());
        assertEquals(5, entries.size());
        assertEquals(
                upis,
                entries.stream()
                        .map(e -> e.getUniParcId().getValue())
                        .collect(Collectors.toList()));
        verify(storeStreamer, times(1)).idsToStoreStream(any(), any());
    }
}
