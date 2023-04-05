package org.uniprot.api.uniprotkb.service;

import static java.util.Collections.*;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/** @author tibrahim */
@ExtendWith(MockitoExtension.class)
public class UniProtEntryServiceTest {

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
    @Mock private RDFStreamer uniProtRDFStreamer;
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
                        uniProtRDFStreamer);
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

    private void mockSolrRequest() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        SearchFieldItem accessionIdSearchField =
                searchFieldConfig.getSearchFieldItemByName("accession_id");
        SearchFieldItem isoFormSearchField =
                searchFieldConfig.getSearchFieldItemByName("is_isoform");
        SearchFieldItem accessionSearchField =
                searchFieldConfig.getSearchFieldItemByName("accession");

        Mockito.when(uniProtKBQueryProcessorConfig.getOptimisableFields())
                .thenReturn(List.of(accessionSearchField));
        Mockito.when(uniProtKBQueryProcessorConfig.getSearchFieldsNames()).thenReturn(EMPTY_SET);
        Mockito.when(uniProtKBQueryProcessorConfig.getLeadingWildcardFields())
                .thenReturn(EMPTY_SET);
        Mockito.when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("accession_id"))
                .thenReturn(accessionIdSearchField);
        Mockito.when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("is_isoform"))
                .thenReturn(isoFormSearchField);
    }
}
