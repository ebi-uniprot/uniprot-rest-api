package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.UniParcStoreClient;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.EnumDisplay;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 19/08/2020
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcControllerGetBySequenceIT {

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private UniParcQueryRepository repository;

    @Autowired private MockMvc mockMvc;

    private static final String getBySequencePath = "/uniparc/sequence";
    private static final String SEQUENCE = "MLMPKRTKYRA";

    private static final String UPI_PREF = "UPI0000083D";
    private static final String ACCESSION = "P12301";
    private static final String UNIPARC_ID = "UPI0000083D01";

    protected void saveEntry() {
        UniParcEntry entry = UniParcEntryMocker.createEntry(1, UPI_PREF);
        // append two more cross ref
        UniParcEntry updatedEntry = UniParcEntryMocker.appendMoreXRefs(entry, 1);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(updatedEntry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC, updatedEntry);
    }

    @BeforeAll
    void initDataStore() {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC));

        UniParcStoreClient storeClient =
                new UniParcStoreClient(VoldemortInMemoryUniParcEntryStore.getInstance("uniparc"));
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC, storeClient);

        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPARC,
                new UniParcDocumentConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));

        saveEntry();
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @Test
    void testGetByNonExistingSequenceNotFound() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath).param("sequence", "AAAAAA"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.*", Matchers.contains("Resource not found")));
    }

    @Test
    void testGetBySequenceWithoutSequenceBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath).param("dbTypes", "invalid"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                containsInAnyOrder(
                                        "'sequence' is a required parameter",
                                        "'invalid' is an invalid UniParc cross reference database name")));
    }

    @Test
    void testGetBySequenceBadRequest() throws Exception {
        // when
        String taxIds =
                IntStream.range(0, 102).mapToObj(String::valueOf).collect(Collectors.joining(","));
        String dbTypes =
                Arrays.stream(UniParcDatabase.values())
                        .map(EnumDisplay::getDisplayName)
                        .collect(Collectors.joining(","));
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", "AA1BBCC")
                                .param("taxonIds", taxIds)
                                .param("fields", "invalid")
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(3)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                containsInAnyOrder(
                                        "'100' is the maximum count limit of comma separated items for 'taxonIds' param. You have passed '102' items.",
                                        "Protein sequence has invalid format. It should match the following regex '[A-Z]+'.",
                                        "Invalid fields parameter value 'invalid'")));
    }

    @Test
    void testGetBySequenceSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath).param("sequence", SEQUENCE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].version", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].versionI", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].active", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].created", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].lastUpdated", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].proteomeId", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].proteomeId", hasItem("UP123456701")))
                .andExpect(jsonPath("uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "uniParcCrossReferences[*].organism.scientificName",
                                notNullValue()))
                .andExpect(
                        jsonPath("uniParcCrossReferences[*].organism.commonName", notNullValue()))
                .andExpect(jsonPath("uniParcCrossReferences[*].organism.taxonId", notNullValue()))
                .andExpect(jsonPath("sequence", notNullValue()))
                .andExpect(jsonPath("sequence.value", is(SEQUENCE)))
                .andExpect(jsonPath("sequence.length", notNullValue()))
                .andExpect(jsonPath("sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("sequence.crc64", notNullValue()))
                .andExpect(jsonPath("sequence.md5", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("sequenceFeatures[*].database", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].databaseId", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].locations", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[0].locations", iterableWithSize(2)))
                .andExpect(jsonPath("sequenceFeatures[*].locations[*].start", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].locations[*].end", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].interproGroup", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].interproGroup.id", notNullValue()))
                .andExpect(jsonPath("sequenceFeatures[*].interproGroup.name", notNullValue()));
    }

    @Test
    void testGetBySequenceWithFieldsSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("fields", "sequence,organism"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].proteomeId").doesNotExist())
                .andExpect(jsonPath("$.uniParcCrossReferences[*].proteinName").doesNotExist())
                .andExpect(jsonPath("$.sequenceFeatures").doesNotExist());
    }

    @Test
    void testGetBySequenceWithDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }

    @Test
    void testGetBySequenceWithInvalidDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "randomDB";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "is an invalid UniParc cross reference database name")));
    }

    @Test
    void testGetBySequenceWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String taxonIds = "9606,55555";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("taxonIds", taxonIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[0].organism.taxonId", Matchers.is(9606)));
    }

    @Test
    void testGetBySequenceWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "true";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(5)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].taxonomy", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].active",
                                contains(true, true, true, true, true)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }

    @Test
    void testGetBySequenceWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "false";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getBySequencePath)
                                .param("sequence", SEQUENCE)
                                .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].taxonomy", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }
}
