package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.createEntryLightWithSequenceLength;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.createUniParcEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 17/08/2020
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcBestGuessControllerIT extends AbstractStreamControllerIT {

    private static final String BEST_GUESS_PATH = "/uniparc/bestguess";

    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());
    @Autowired UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;
    @Autowired UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private SolrClient solrClient;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @BeforeAll
    void initUniParcBestGuessDataStore() throws IOException, SolrServerException {
        saveEntries();

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    @Test
    void bestGuessCanReturnSuccessSwissProt() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("upis", "UPI0000183A10");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A10")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database", is("UniProtKB/Swiss-Prot")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("swissProt0")));
    }

    @Test
    void bestGuessCanReturnSuccessTrembl() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("upis", "UPI0000183A11,UPI0000183A12");

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A12")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].database", is("UniProtKB/TrEMBL")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("trembl2")));
    }

    @Test
    void bestGuessCanReturnIsoform() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("upis", "UPI0000183A11,UPI0000183A12,UPI0000183A13");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A13")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database",
                                is("UniProtKB/Swiss-Prot protein isoforms")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("isoform3")));
    }

    @Test
    void bestGuessCanReturnSuccessFilteringTaxonomy() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("taxonIds", "9609");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A13")))
                .andExpect(jsonPath("$.uniParcCrossReferences.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].database",
                                is("UniProtKB/Swiss-Prot protein isoforms")))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].id", is("isoform3")));
    }

    @Test
    void bestGuessCanReturnSuccessWithFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("dbids", "trembl2")
                        .param("taxonIds", "9607")
                        .param("fields", "upi,sequence");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.uniParcId", is("UPI0000183A12")))
                .andExpect(jsonPath("$.sequence").exists())
                .andExpect(jsonPath("$.uniParcCrossReferences").doesNotExist());
    }

    @Test
    void bestGuessReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("invalid_query", "9607")
                        .param("fields", "invalid");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalid'",
                                        "Provide at least one of 'upis', 'accessions', 'dbids', 'genes', or 'taxonIds'. 'dbids' alone is not allowed.")));
    }

    @Test
    void bestGuessReturnBadRequestWhenOnlyDbIdsPassed() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("dbids", "sample1,sample2");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Provide at least one of 'upis', 'accessions', 'dbids', 'genes', or 'taxonIds'. 'dbids' alone is not allowed.")));
    }

    @Test
    void bestGuessReturnBadRequestWhenInvalidTaxonIdPassed() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("taxonIds", "1234,invalidTaxonId");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "taxonIds value has invalid format. It should be a list of comma separated taxonIds (without spaces).")));
    }

    @Test
    void bestGuessReturnBadRequestIfFoundDuplicatedEntries() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(BEST_GUESS_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("taxonIds", "9607");

        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. More than one Best Guess found {UPI0000183A10:trembl0;UPI0000183A13:trembl3;UPI0000183A12:trembl2}. Review your query and/or contact us.")));
    }

    private void saveEntries() throws IOException, SolrServerException {
        UniParcEntryConverter converter = new UniParcEntryConverter();

        // SWISSPROT
        String uniParcId = "UPI0000183A10";
        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT, "swissProt0", 9606, true));
        crossReferences.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.TREMBL, "trembl0", 9607, true));
        crossReferences.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive0", 9608, false));
        crossReferences.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "EMBL0", 9609, true));

        UniParcEntry entry = createUniParcEntry(uniParcId, 20, crossReferences);
        UniParcDocument doc = documentConverter.convert(converter.toXml(entry));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        // save uniparc light in store
        UniParcEntryLight lightEntry =
                UniParcEntryMocker.createEntryLightWithSequenceLength(
                        uniParcId, 20, crossReferences.size());
        uniParcLightStoreClient.saveEntry(lightEntry);
        // save cross references in store
        String xrefBatchKey = uniParcId + "_0";
        uniParcCrossReferenceStoreClient.saveEntry(
                new UniParcCrossReferencePair(xrefBatchKey, crossReferences));

        // SWISSPROT - SMALLER SEQUENCE
        String uniParcId1 = "UPI0000183A11";
        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT, "swissProt1", 9606, true));
        crossReferences1.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.TREMBL, "trembl1", 9607, true));
        crossReferences1.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive1", 9608, false));
        crossReferences1.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "EMBL1", 9609, true));

        UniParcEntry entry1 = createUniParcEntry(uniParcId1, 19, crossReferences1);
        UniParcDocument doc1 = documentConverter.convert(converter.toXml(entry1));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc1);
        // save uniparc light in store
        UniParcEntryLight lightEntry1 =
                createEntryLightWithSequenceLength(uniParcId1, 19, crossReferences1.size());
        uniParcLightStoreClient.saveEntry(lightEntry1);
        // save cross references in store
        String xrefBatchKey1 = uniParcId1 + "_0";
        uniParcCrossReferenceStoreClient.saveEntry(
                new UniParcCrossReferencePair(xrefBatchKey1, crossReferences1));

        // TREMBL
        String uniParcId2 = "UPI0000183A12";
        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.TREMBL, "trembl2", 9607, true));
        crossReferences2.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.TREMBL, "inactive2", 9608, false));
        crossReferences2.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "EMBL2", 9609, true));

        UniParcEntry entry2 = createUniParcEntry(uniParcId2, 20, crossReferences2);
        UniParcDocument doc2 = documentConverter.convert(converter.toXml(entry2));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc2);
        // save uniparc light in store
        UniParcEntryLight lightEntry2 =
                createEntryLightWithSequenceLength(uniParcId2, 20, crossReferences2.size());
        uniParcLightStoreClient.saveEntry(lightEntry2);
        // save cross references in store
        String xrefBatchKey2 = uniParcId2 + "_0";
        uniParcCrossReferenceStoreClient.saveEntry(
                new UniParcCrossReferencePair(xrefBatchKey2, crossReferences2));

        // ISOFORM
        String uniParcId3 = "UPI0000183A13";
        List<UniParcCrossReference> crossReferences3 = new ArrayList<>();
        crossReferences3.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT_VARSPLIC, "isoform3", 9609, true));
        crossReferences3.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.TREMBL, "trembl3", 9607, true));
        crossReferences3.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.SWISSPROT, "swissProtInactive3", 9608, false));
        crossReferences3.add(
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "EMBL3", 9610, true));
        UniParcEntry entry3 = createUniParcEntry(uniParcId3, 20, crossReferences3);
        UniParcDocument doc3 = documentConverter.convert(converter.toXml(entry3));
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc3);

        // save uniparc light in store
        UniParcEntryLight lightEntry3 =
                createEntryLightWithSequenceLength(uniParcId3, 20, crossReferences3.size());
        uniParcLightStoreClient.saveEntry(lightEntry3);
        // save cross references in store
        String xrefBatchKey3 = uniParcId3 + "_0";
        uniParcCrossReferenceStoreClient.saveEntry(
                new UniParcCrossReferencePair(xrefBatchKey3, crossReferences3));
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }
}
