package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.entry.UniParcStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortInMemoryUniParcCrossReferenceStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 17/08/2020
 */
abstract class AbstractGetSingleUniParcByIdTest extends AbstractGetByIdControllerIT {

    @Autowired private UniParcQueryRepository repository;

    protected static final String UPI_PREF = "UPI0000083D";
    protected static String ACCESSION = "P12301";
    protected static String UNIPARC_ID = "UPI0000083D01";
    private UniParcStoreClient storeClient;

    protected abstract String getIdPathValue();

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPARC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniparc;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return this.repository;
    }

    @Override
    protected void saveEntry() {
        int xrefCount = 25;
        // full uniparc entry object for solr
        UniParcEntry uniParcEntry = UniParcEntryMocker.createUniParcEntry(1, UPI_PREF, xrefCount);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(uniParcEntry);
        DataStoreManager manager = getStoreManager();
        manager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        //  uniparc light and cross reference in voldemort
        int qualifier = 1;
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(qualifier, UPI_PREF, xrefCount);
        manager.saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcEntryLight);
        List<UniParcCrossReferencePair> crossReferences =
                UniParcCrossReferenceMocker.createUniParcCrossReferencePairs(
                        uniParcEntryLight.getUniParcId(), 1, xrefCount, xrefGroupSize);
        manager.saveToStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossReferences);
    }

    @BeforeAll
    void initDataStore() {
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIPARC,
                        new UniParcDocumentConverter(
                                TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));

        UniParcLightStoreClient uniParcLightStoreClient =
                new UniParcLightStoreClient(
                        VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
        VoldemortInMemoryUniParcCrossReferenceStore xrefVDClient =
                VoldemortInMemoryUniParcCrossReferenceStore.getInstance("uniparc-cross-reference");
        UniParcCrossReferenceStoreClient crossRefStoreClient =
                new UniParcCrossReferenceStoreClient(xrefVDClient);
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcLightStoreClient);
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossRefStoreClient);
    }

    @AfterAll
    void cleanUp() {
        getStoreManager().cleanSolr(DataStoreManager.StoreType.UNIPARC);
        getStoreManager().close();
    }

    @Test
    void testGetByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(8)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].database",
                                containsInAnyOrder(
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL")))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }

    @Test
    void testGetByAccessionWithInvalidDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "randomDB";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
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
    void testGetByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String taxonIds = "9606,5555";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("taxonIds", taxonIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism.taxonId", hasItem(9606)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }

    @Test
    void testGetByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "true";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(21)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", everyItem(is(true))))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }

    @Test
    void testGetByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "false";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", everyItem(is(false))))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
    }
}
