package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.contains;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.flatfile.writer.LineType;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchByMnemonicIT {
    private static final String ACC_LINE = "AC   %s;";
    private static final String UNIPROT_FLAT_FILE_ENTRY_PATH = "/it/P0A377.43.dat";
    private static final String TARGET_ACCESSION = "Q197F5";
    private static final String TARGET_ID = "CYC_HUMAN";
    private static final String ID_LINE = "ID   %s               Reviewed;         105 AA.";

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private UniprotQueryRepository repository;

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeAll
    void setUp() throws IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPROT));

        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

        storeManager.addDocConverter(DataStoreManager.StoreType.UNIPROT, uniProtEntryConverter);

        UniProtKBStoreClient storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
        InputStream resourceAsStream = TestUtils.getResourceAsStream(UNIPROT_FLAT_FILE_ENTRY_PATH);
        UniProtEntryObjectProxy entryProxy =
                UniProtEntryObjectProxy.createEntryFromInputStream(resourceAsStream);

        // Entry 1
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, "Q197F4"));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, "CYC_PANTR"));
        storeManager.save(
                DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 2
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, TARGET_ACCESSION));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, TARGET_ID));
        storeManager.save(
                DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 3
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, "Q197F6"));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, "AATM_RABIT"));
        storeManager.save(
                DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));
    }

    @Test
    void canReachSearchEndpoint() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("query", "id:" + TARGET_ID));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    void canReachSearchEndpointMixCase() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("query", "id:cYc_Human"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    void canReachSearchEndpointWithDefault() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("query", "id_default:AATM_RABIT"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q197F6")));
    }

    @Test
    void canReachSearchEndpointPartNotAvailable() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("query", "id:AATM"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results.*.primaryAccession")
                                .doesNotExist());
    }
}
