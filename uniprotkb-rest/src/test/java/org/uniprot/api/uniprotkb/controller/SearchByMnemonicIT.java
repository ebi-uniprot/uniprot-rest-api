package org.uniprot.api.uniprotkb.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.flatfile.writer.LineType;
import org.uniprot.store.indexer.DataStoreManager;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class SearchByMnemonicIT {
    public static final String ACC_LINE = "AC   %s;";
    private static final String UNIPROT_FLAT_FILE_ENTRY_PATH = "/it/P0A377.43.dat";
    private static final String TARGET_ACCESSION = "Q197F5";
    private static final String TARGET_ID = "CYC_HUMAN";
    private static final String ID_LINE = "ID   %s               Reviewed;         105 AA.";

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    @Autowired
    private DataStoreManager storeManager;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static MockMvc mockMvc;

    @Before
    public void setUp() throws IOException {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
        InputStream resourceAsStream = TestUtils.getResourceAsStream(UNIPROT_FLAT_FILE_ENTRY_PATH);
        UniProtEntryObjectProxy entryProxy = UniProtEntryObjectProxy.createEntryFromInputStream(resourceAsStream);

        //Entry 1
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, "Q197F4"));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, "CYC_PANTR"));
        storeManager.save(DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));

        //Entry 2
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, TARGET_ACCESSION));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, TARGET_ID));
        storeManager.save(DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));

        //Entry 3
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, "Q197F6"));
        entryProxy.updateEntryObject(LineType.ID, String.format(ID_LINE, "AATM_RABIT"));
        storeManager.save(DataStoreManager.StoreType.UNIPROT, TestUtils.convertToUniProtEntry(entryProxy));
    }

    @Test
    public void canReachSearchEndpoint() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "mnemonic:" + TARGET_ID));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    public void canReachSearchEndpointMixCase() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "mnemonic:cYc_Human"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(TARGET_ACCESSION)));
    }

    @Test
    public void canReachSearchEndpointWithDefault() throws Exception {
    
       // when
       ResultActions response = mockMvc.perform(
               get(SEARCH_RESOURCE)
                       .header(ACCEPT, APPLICATION_JSON_VALUE)
                       .param("query", "mnemonic_default:AATM_RABIT"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q197F6")));
    }

    @Test
    public void canReachSearchEndpointPartNotAvailable() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "mnemonic:AATM"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))

       .andExpect(jsonPath("$.results.*.primaryAccession").doesNotExist());
   }
}
