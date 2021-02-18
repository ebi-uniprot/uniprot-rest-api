package org.uniprot.api.idmapping.controller;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.idmapping.IDMappingREST;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IDMappingREST.class})
@WebMvcTest(UniProtKBIdMappingController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingControllerIT extends AbstractStreamControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_SEARCH = "/uniprotkb/idmapping/search";

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot);
    }

    @Test
    void testUniProtKBToUniProtKBMapping() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("ids", "Q65PH2,Q81VZ2,Q15796"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }
}
