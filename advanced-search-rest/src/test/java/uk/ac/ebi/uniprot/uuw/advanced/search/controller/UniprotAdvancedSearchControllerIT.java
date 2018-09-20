package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdvancedSearchREST.class, properties = "spring.http.encoding.enabled=false")
@WebAppConfiguration
@Import({DataStoreTestConfig.class})
@ActiveProfiles("hello")
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

        mockMvc.perform(get("/uniprot/search?query=accession:*")).andDo(print());
    }
}