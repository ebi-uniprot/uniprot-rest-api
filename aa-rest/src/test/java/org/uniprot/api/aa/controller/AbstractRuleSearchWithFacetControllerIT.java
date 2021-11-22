package org.uniprot.api.aa.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;

/**
 * @author sahmad
 * @created 19/11/2021
 */
public abstract class AbstractRuleSearchWithFacetControllerIT
        extends AbstractSearchWithFacetControllerIT {
    @Test
    void searchWithFacetAsSearchField() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "superkingdom:Eukaryota")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }
}
