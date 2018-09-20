package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.AdvancedSearchREST;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreTestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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

        mockMvc.perform(
                get("/uniprot/search?query=accession:*"))
                .andDo(print())
                // see uk.ac.ebi.quickgo.annotation.controller.AnnotationControllerDownloadIT#checkResponse
                // i.e., check: vary header is present, content type is correct, content string contains
                // some text we're looking for
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }
}