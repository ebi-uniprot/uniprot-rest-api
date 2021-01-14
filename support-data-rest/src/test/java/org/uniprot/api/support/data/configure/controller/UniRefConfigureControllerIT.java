package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;

/**
 * @author lgonzales
 * @since 28/05/2020
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(UniRefConfigureController.class)
class UniRefConfigureControllerIT {

    private static final String CONFIGURE_RESOURCE = "/configure/uniref/";

    @Autowired private MockMvc mockMvc;

    @Test
    void validResultFields() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(CONFIGURE_RESOURCE + "result-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }

    @Test
    void validSearchFields() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(CONFIGURE_RESOURCE + "search-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }
}
