package org.uniprot.api.uniprotkb.service;

import static java.util.Collections.EMPTY_SET;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.common.repository.search.SolrQueryConverter.DEF_TYPE;
import static org.uniprot.api.common.repository.search.SolrQueryConverter.FILTER_QUERY;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/** @author tibrahim */
@ExtendWith(MockitoExtension.class)
class UniProtEntryServiceTest {

    @Mock private UniprotQueryRepository repository;
    @Mock private UniProtKBFacetConfig uniprotKBFacetConfig;
    @Mock private UniProtTermsConfig uniProtTermsConfig;
    @Mock private UniProtSolrSortClause uniProtSolrSortClause;
    @Mock private SolrQueryConfig uniProtKBSolrQueryConf;
    @Mock private UniProtKBStoreClient entryStore;
    @Mock private StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer;
    @Mock private TaxonomyLineageService taxService;
    @Mock private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Mock private UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig;
    @Mock private SearchFieldConfig uniProtKBSearchFieldConfig;
    @Mock private RdfStreamer uniProtRdfStreamer;
    @Mock private TupleStreamDocumentIdStream documentIdStream;
    private UniProtEntryService entryService;

    @BeforeEach
    void init() {
        entryService =
                new UniProtEntryService(
                        repository,
                        uniprotKBFacetConfig,
                        uniProtTermsConfig,
                        uniProtSolrSortClause,
                        uniProtKBSolrQueryConf,
                        entryStore,
                        uniProtEntryStoreStreamer,
                        taxService,
                        facetTupleStreamTemplate,
                        uniProtKBQueryProcessorConfig,
                        uniProtKBSearchFieldConfig,
                        documentIdStream,
                        uniProtRdfStreamer);
    }

    @Test
    void changeLowercaseAccessionToUppercase() {
        mockSolrRequest();
        UniProtKBSearchRequest verifyValidSearchRequest = new UniProtKBSearchRequest();
        verifyValidSearchRequest.setQuery("p12345");
        verifyValidSearchRequest.setSize(10);
        SolrRequest verifyValidSolrRequest =
                entryService.createSearchSolrRequest(verifyValidSearchRequest);
        assertNotNull(verifyValidSolrRequest);
        assertEquals("P12345", verifyValidSolrRequest.getQuery());
    }

    @Test
    void changeQueryToUpperCaseOnlyIfItIsAccession() {
        mockSolrRequest();
        UniProtKBSearchRequest verifyFailingRequest = new UniProtKBSearchRequest();
        verifyFailingRequest.setQuery("hexdecimalString134");
        verifyFailingRequest.setSize(10);
        SolrRequest verifyFailingSolrRequestCase =
                entryService.createSearchSolrRequest(verifyFailingRequest);
        assertNotNull(verifyFailingSolrRequestCase);
        assertEquals("hexdecimalString134", verifyFailingSolrRequestCase.getQuery());
    }

    @Test
    void verifyQueryHasAccessionRegexAndHasDash() {
        mockSolrRequest();
        UniProtKBSearchRequest verifyValidSearchRequest = new UniProtKBSearchRequest();
        verifyValidSearchRequest.setQuery("p12345-1");
        verifyValidSearchRequest.setSize(10);
        SolrRequest verifyValidSolrRequest =
                entryService.createSearchSolrRequest(verifyValidSearchRequest);
        assertNotNull(verifyValidSolrRequest);
        assertTrue(verifyValidSolrRequest.getFilterQueries().isEmpty());
    }

    @Test
    void addIsoFormFalseFilterOnlyIfQueryHasNoAccessionValue() {
        mockSolrRequest();
        UniProtKBSearchRequest verifyFailingRequest = new UniProtKBSearchRequest();
        verifyFailingRequest.setQuery("accession:hexdecimalString134");
        verifyFailingRequest.setSize(10);
        SolrRequest verifyFailingSolrRequestCase =
                entryService.createSearchSolrRequest(verifyFailingRequest);
        assertNotNull(verifyFailingSolrRequestCase);
        assertTrue(!verifyFailingSolrRequestCase.getFilterQueries().isEmpty());
    }

    @Test
    void search_in_list_format_without_voldemort_being_called() {
        mockSolrRequest();
        // when
        String acc1 = "P12345";
        String acc2 = "P54321";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = acc1;
        UniProtDocument doc2 = new UniProtDocument();
        doc2.accession = acc2;
        UniProtKBEntry entry1 =
                new UniProtKBEntryBuilder(acc1, acc1, UniProtKBEntryType.SWISSPROT).build();
        UniProtKBEntry entry2 =
                new UniProtKBEntryBuilder(acc2, acc2, UniProtKBEntryType.SWISSPROT).build();
        QueryResult<UniProtDocument> solrDocs =
                QueryResult.<UniProtDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(CursorPage.of("", 1, 2))
                        .build();
        UniProtKBSearchRequest request = new UniProtKBSearchRequest();
        request.setQuery("field:value");
        request.setSize(10);
        request.setFormat(LIST_MEDIA_TYPE_VALUE);
        when(repository.searchPage(any(), any())).thenReturn(solrDocs);
        QueryResult<UniProtKBEntry> result = entryService.search(request);
        List<UniProtKBEntry> entries = result.getContent().collect(Collectors.toList());
        verify(entryStore, never()).getEntry(any());
        assertEquals(2, entries.size());
        assertEquals(entry1, entries.get(0));
        assertEquals(entry2, entries.get(1));
    }

    @Test
    void search_in_non_list_format_with_voldemort_being_called() {
        mockSolrRequest();
        // when
        String acc1 = "Q56789";
        String acc2 = "P56789";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = acc1;
        UniProtDocument doc2 = new UniProtDocument();
        doc2.accession = acc2;
        UniProtKBEntry entry1 =
                new UniProtKBEntryBuilder(acc1, acc1, UniProtKBEntryType.SWISSPROT).build();
        UniProtKBEntry entry2 =
                new UniProtKBEntryBuilder(acc2, acc2, UniProtKBEntryType.SWISSPROT).build();
        QueryResult<UniProtDocument> solrDocs =
                QueryResult.<UniProtDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(CursorPage.of("", 2))
                        .build();
        UniProtKBSearchRequest request = new UniProtKBSearchRequest();
        request.setQuery("field2:value");
        request.setSize(10);
        request.setFormat(TSV_MEDIA_TYPE_VALUE);
        when(repository.searchPage(any(), any())).thenReturn(solrDocs);
        doReturn(Optional.of(entry1)).when(entryStore).getEntry(acc1);
        doReturn(Optional.of(entry2)).when(entryStore).getEntry(acc2);
        QueryResult<UniProtKBEntry> result = entryService.search(request);
        List<UniProtKBEntry> entries = result.getContent().collect(Collectors.toList());
        verify(entryStore, times(2)).getEntry(any());
        assertEquals(2, entries.size());
        assertEquals(entry1, entries.get(0));
    }

    @Test
    void stream_in_list_format_without_voldemort_being_called() {
        mockSolrRequest();
        // when
        String acc1 = "Q12345";
        String acc2 = "Q54321";
        UniProtKBEntry entry1 =
                new UniProtKBEntryBuilder(acc1, acc1, UniProtKBEntryType.SWISSPROT).build();
        UniProtKBEntry entry2 =
                new UniProtKBEntryBuilder(acc2, acc2, UniProtKBEntryType.SWISSPROT).build();
        UniProtKBStreamRequest request = new UniProtKBStreamRequest();
        request.setQuery("field1:value1");
        request.setFormat(LIST_MEDIA_TYPE_VALUE);
        when(documentIdStream.fetchIds(any())).thenReturn(Stream.of(acc1, acc2));
        Stream<UniProtKBEntry> result = entryService.stream(request);
        List<UniProtKBEntry> entries = result.collect(Collectors.toList());
        verify(entryStore, never()).getEntries(any());
        assertEquals(2, entries.size());
        assertEquals(entry1, entries.get(0));
        assertEquals(entry2, entries.get(1));
    }

    @Test
    void getFacets_filterIsoforms() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        List<FacetField> facetFields = mock(List.class);
        when(queryResponse.getFacetFields()).thenReturn(facetFields);
        SearchFieldItem searchFieldItem = mock(SearchFieldItem.class);
        when(searchFieldItem.getFieldName()).thenReturn("fieldName");
        when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("is_isoform"))
                .thenReturn(searchFieldItem);
        when(repository.query(
                        argThat(
                                solrQuery ->
                                        solrQuery.get(FacetParams.FACET).equals("true")
                                                && solrQuery.get(DEF_TYPE).equals("edismax")
                                                && solrQuery
                                                        .get(FILTER_QUERY)
                                                        .contains("fieldName"))))
                .thenAnswer(invocation -> queryResponse);

        List<FacetField> facets = entryService.getFacets("query", Map.of());

        assertSame(facetFields, facets);
    }

    @Test
    void getFacets() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        List<FacetField> facetFields = mock(List.class);
        when(queryResponse.getFacetFields()).thenReturn(facetFields);
        when(repository.query(
                        argThat(
                                solrQuery ->
                                        solrQuery.get(FacetParams.FACET).equals("true")
                                                && solrQuery.get(DEF_TYPE).equals("edismax"))))
                .thenAnswer(invocation -> queryResponse);

        List<FacetField> facets = entryService.getFacets("O38XU3-3742170", Map.of());

        assertSame(facetFields, facets);
    }

    @Test
    void stream_in_non_list_format_with_voldemort_store_streamer_being_called() {
        mockSolrRequest();
        // when
        String acc1 = "P56789";
        UniProtKBEntry entry1 =
                new UniProtKBEntryBuilder(acc1, acc1, UniProtKBEntryType.SWISSPROT).build();
        UniProtKBStreamRequest request = new UniProtKBStreamRequest();
        request.setQuery("field1:value1");
        request.setFormat(APPLICATION_JSON_VALUE);
        when(uniProtEntryStoreStreamer.idsToStoreStream(any(), any()))
                .thenReturn(Stream.of(entry1));
        Stream<UniProtKBEntry> result = entryService.stream(request);
        List<UniProtKBEntry> entries = result.collect(Collectors.toList());
        verify(uniProtEntryStoreStreamer, times(1)).idsToStoreStream(any(), any());
        assertEquals(1, entries.size());
        assertEquals(entry1, entries.get(0));
    }

    @Test
    void findAccessionByProteinIdReturnSuccess() {
        String proteinId = "PROTEIN_ID";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = "DOC1";
        doc1.id.add(proteinId);
        doc1.active = true;
        Page page = CursorPage.of(null, 10, 1);
        QueryResult<UniProtDocument> queryResult =
                QueryResult.<UniProtDocument>builder().content(Stream.of(doc1)).page(page).build();
        Mockito.when(repository.searchPage(any(), any())).thenReturn(queryResult);
        String result = entryService.findAccessionByProteinId(proteinId);
        assertNotNull(result);
        assertEquals("DOC1", result);
    }

    @Test
    void findAccessionByProteinIdReturnProteinIdThatHasProteinAddedDuringFlatFileConvert() {
        String proteinId = "PROTEIN_ID";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = "DOC1";
        doc1.id.add(proteinId);
        doc1.active = true;
        UniProtDocument doc2 = new UniProtDocument();
        doc2.id.add("OTHER_ID");
        doc2.id.add(proteinId); // added by idTracker file logic
        doc2.active = true;
        doc2.accession = "DOC2";
        Page page = CursorPage.of(null, 10, 2);
        QueryResult<UniProtDocument> queryResult =
                QueryResult.<UniProtDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(page)
                        .build();
        Mockito.when(repository.searchPage(any(), any())).thenReturn(queryResult);
        String result = entryService.findAccessionByProteinId(proteinId);
        assertNotNull(result);
        assertEquals("DOC1", result);
    }

    @Test
    void findAccessionByProteinIdReturnProteinIdThatIsActive() {
        String proteinId = "PROTEIN_ID";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = "DOC1";
        doc1.id.add(proteinId);
        doc1.active = true;
        UniProtDocument doc2 = new UniProtDocument();
        doc2.id.add(proteinId);
        doc2.active = false;
        doc2.accession = "DOC2";
        Page page = CursorPage.of(null, 10, 2);
        QueryResult<UniProtDocument> queryResult =
                QueryResult.<UniProtDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(page)
                        .build();
        Mockito.when(repository.searchPage(any(), any())).thenReturn(queryResult);
        String result = entryService.findAccessionByProteinId(proteinId);
        assertNotNull(result);
        assertEquals("DOC1", result);
    }

    @Test
    void findAccessionByProteinIdMultipleEntries() {
        // This use case should not have with real data..
        String proteinId = "PROTEIN_ID";
        UniProtDocument doc1 = new UniProtDocument();
        doc1.accession = "DOC1";
        doc1.id.add(proteinId);
        doc1.active = true;
        UniProtDocument doc2 = new UniProtDocument();
        doc2.id.add(proteinId);
        doc2.active = true;
        doc2.accession = "DOC2";
        Page page = CursorPage.of(null, 10, 2);
        QueryResult<UniProtDocument> queryResult =
                QueryResult.<UniProtDocument>builder()
                        .content(Stream.of(doc1, doc2))
                        .page(page)
                        .build();
        Mockito.when(repository.searchPage(any(), any())).thenReturn(queryResult);
        assertThrows(
                ServiceException.class, () -> entryService.findAccessionByProteinId("PROTEIN_ID"));
    }

    @Test
    void findAccessionByProteinIdNotFound() {
        Page page = CursorPage.of(null, 10, 0);
        QueryResult<UniProtDocument> queryResult =
                QueryResult.<UniProtDocument>builder().content(Stream.empty()).page(page).build();
        Mockito.when(repository.searchPage(any(), any())).thenReturn(queryResult);
        assertThrows(
                ResourceNotFoundException.class,
                () -> entryService.findAccessionByProteinId("PROTEIN_ID"));
    }

    private void mockSolrRequest() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        SearchFieldItem accessionIdSearchField =
                searchFieldConfig.getSearchFieldItemByName("accession_id");
        SearchFieldItem isoFormSearchField =
                searchFieldConfig.getSearchFieldItemByName("is_isoform");
        SearchFieldItem accessionSearchField =
                searchFieldConfig.getSearchFieldItemByName("accession");

        when(uniProtKBQueryProcessorConfig.getOptimisableFields())
                .thenReturn(List.of(accessionSearchField));
        when(uniProtKBQueryProcessorConfig.getSearchFieldsNames()).thenReturn(EMPTY_SET);
        when(uniProtKBQueryProcessorConfig.getLeadingWildcardFields()).thenReturn(EMPTY_SET);
        when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("accession_id"))
                .thenReturn(accessionIdSearchField);
        when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("is_isoform"))
                .thenReturn(isoFormSearchField);
    }
}
