package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.AdvancedSearchREST;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreTestConfig;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, AdvancedSearchREST.class})
@WebAppConfiguration
public class UniprotAdvancedSearchControllerIT {
    private static final String ACCESSION_RESOURCE = UNIPROTKB_RESOURCE + "/accession/";
    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

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
    public void canReachAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void canFilterEntryFromAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("fields", "gene")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.accession", contains(acc)))
                .andExpect(jsonPath("$.results.*.gene").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.lineage").doesNotExist());
    }

    @Test
    public void canReachSearchEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void canFilterEntryFromSearchEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("query", "accession:" + acc)
                        .param("fields", "gene")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.accession", contains(acc)))
                .andExpect(jsonPath("$.results.*.gene").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.lineage").doesNotExist());
    }

    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }
}