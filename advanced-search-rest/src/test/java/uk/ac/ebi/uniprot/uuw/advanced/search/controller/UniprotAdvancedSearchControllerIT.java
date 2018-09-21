package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.AdvancedSearchREST;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreTestConfig;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.FlatFileMessageConverter.FF_MEDIA_TYPE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AdvancedSearchREST.class,
                           DataStoreTestConfig.class}, properties = "spring.http.encoding.enabled=false")
@WebAppConfiguration
public class UniprotAdvancedSearchControllerIT {
    @Autowired
    private DataStoreManager storeManager;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void canReachDownloadEndpoint() throws Exception {
        storeManager.save(DataStoreManager.StoreType.UNIPROT, UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));

        ResultActions response = mockMvc.perform(
                get("/uniprot/download?query=accession:Q8DIA7")
                        .header(ACCEPT, FF_MEDIA_TYPE));

        response.andExpect(
                request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andDo(print())
                .andExpect(content().contentType(FF_MEDIA_TYPE))
                .andExpect(content().string(containsString("AC   Q8DIA7;")))
                .andExpect(header().stringValues(VARY, ACCEPT, ACCEPT_ENCODING))
                .andExpect(header().exists(CONTENT_DISPOSITION));

    }
}