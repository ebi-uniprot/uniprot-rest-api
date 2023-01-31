package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.AsyncDownloadMocks;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.flatfile.writer.LineType;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {DataStoreTestConfig.class, AsyncDownloadMocks.class, UniProtKBREST.class})
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
                new UniProtEntryConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(),
                        Mockito.mock(GORepo.class),
                        PathwayRepoMocker.getPathwayRepo(),
                        Mockito.mock(ChebiRepo.class),
                        Mockito.mock(ECRepo.class),
                        new HashMap<>());

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
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "id:" + TARGET_ID));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    void canReachSearchEndpointMixCase() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "id:cYc_Human"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    void canReachSearchEndpointWithDefault() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "id_default:AATM_RABIT"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q197F6")));
    }

    @Test
    void canReachSearchEndpointPartNotAvailable() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "id:AATM"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession").doesNotExist());
    }
}
