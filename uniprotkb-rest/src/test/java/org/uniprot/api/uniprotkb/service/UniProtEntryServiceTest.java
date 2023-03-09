package org.uniprot.api.uniprotkb.service;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

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
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/** @author tibrahim */
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        SearchFieldItem accessionSearchField = new SearchFieldItem();
        SearchFieldItem isoFormSearchField = new SearchFieldItem();
        UniProtKBSearchRequest request = new UniProtKBSearchRequest();

        request.setQuery("p12345");
        request.setSize(10);

        accessionSearchField.setFieldName("accession_id");
        isoFormSearchField.setFieldName("is_isoform");

        Mockito.when(uniProtKBQueryProcessorConfig.getLeadingWildcardFields())
                .thenReturn(EMPTY_SET);
        Mockito.when(uniProtKBQueryProcessorConfig.getOptimisableFields())
                .thenReturn(Collections.emptyList());
        Mockito.when(uniProtKBQueryProcessorConfig.getSearchFieldsNames()).thenReturn(EMPTY_SET);
        Mockito.when(uniProtKBQueryProcessorConfig.getLeadingWildcardFields())
                .thenReturn(EMPTY_SET);
        Mockito.when(uniProtKBSolrQueryConf.getQueryFields()).thenReturn("");
        Mockito.when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("accession_id"))
                .thenReturn(accessionSearchField);
        Mockito.when(uniProtKBSearchFieldConfig.getSearchFieldItemByName("is_isoform"))
                .thenReturn(isoFormSearchField);

        SolrRequest solrRequest = entryService.createSearchSolrRequest(request);
        assertNotNull(solrRequest);
        assertEquals("P12345", solrRequest.getQuery());
    }
}
