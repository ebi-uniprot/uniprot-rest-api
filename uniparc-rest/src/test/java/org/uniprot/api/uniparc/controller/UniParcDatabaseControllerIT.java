package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;

import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.api.uniparc.repository.store.UniParcStreamConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 29/03/2021
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcDatabaseController.class)
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniParcDatabaseControllerIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();
    private UniParcStoreClient storeClient;
    @Autowired private UniParcQueryRepository repository;
    private static final String UPI_PREF = "UPI00000832";
    @Autowired protected MockMvc mockMvc;

    @BeforeAll
    void init() {
        storeClient =
                new UniParcStoreClient(
                        VoldemortInMemoryUniParcEntryStore.getInstance("avro-uniparc"));
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC));
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC, storeClient);
        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPARC,
                new UniParcDocumentConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));
        saveEntry();
    }

    void saveEntry() {
        UniParcEntry entry = UniParcEntryMocker.createEntry(1, UPI_PREF);
        // append two more cross ref
        UniParcEntry updatedEntry = UniParcEntryMocker.appendMoreXRefs(entry, 1);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(updatedEntry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC, updatedEntry);
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @Test
    void testGetDatabasesByUpiSuccess() throws Exception {
        // when
        String upi = "UPI0000083201";
        ResultActions response = mockMvc.perform(get("/uniparc/{upi}/databases", upi));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()));
    }

    @Test
    void testGetDatabasesByUpiTsvSuccess() throws Exception {
        // when
        String upi = "UPI0000083201";
        ResultActions response =
                mockMvc.perform(
                        get("/uniparc/{upi}/databases", upi).header(ACCEPT, TSV_MEDIA_TYPE_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TSV_MEDIA_TYPE_VALUE));
    }
}
