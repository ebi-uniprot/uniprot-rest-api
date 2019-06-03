package uk.ac.ebi.uniprot.api.uniprotkb.controller;

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
import uk.ac.ebi.uniprot.api.uniprotkb.UniProtKBREST;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.UniProtEntryMocker;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 *
 * @author lgonzales
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class UniprotKBSearchPaginationControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DataStoreManager storeManager;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void checkThatDefaultPageSizeIs25() throws Exception {
        // given
        saveEntries(26);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords","26"))
                .andExpect(header().string(HttpHeaders.LINK,notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK,is("<https://localhost/uniprotkb/search?query=*:*" +
                        "&cursor=tov32hc7k09uw9s4h3dh2mqy7i5vh&size=25>; rel=\"next\"")))
                .andExpect(jsonPath("$.results.size()", is(25)));
    }

    @Test
    public void onlyOnePageSearchWithSize5() throws Exception {
        // given
        saveEntries(5);

        // when page
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*")
                        .param("size","5"));

        // then page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords","5"))
                .andExpect(header().string(HttpHeaders.LINK,nullValue()))
                .andExpect(jsonPath("$.results.size()", is(5)));

    }

    @Test
    public void iterateOverAllPages() throws Exception {
        // given
        saveEntries(6);

        // when first page
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*")
                        .param("size","5"));

        // then first page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords","6"))
                .andExpect(header().string(HttpHeaders.LINK,notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK,is("<https://localhost/uniprotkb/search?query=*:*" +
                        "&cursor=4f6ow1f2ng27h9w20bdqm103jdbx&size=5>; rel=\"next\"")))
                .andExpect(jsonPath("$.results.size()", is(5)));


        // when last page
        response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*")
                        .param("cursor", "4f6ow1f2ng27h9w20bdqm103jdbx")
                        .param("size","5"));

        // then last page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords","6"))
                .andExpect(header().string(HttpHeaders.LINK,nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)));
    }

    private void saveEntries(int numberOfEntries) {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
        for(int i=1;i<=numberOfEntries;i++){
            UniProtEntry uniProtEntry = UniProtEntryMocker.create("P0000"+i);
            storeManager.save(DataStoreManager.StoreType.UNIPROT, uniProtEntry);
        }

    }


}
