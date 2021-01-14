package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

/** @author lgonzales */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(UtilControllerIT.class)
class UtilControllerIT {

    private static final String PARSE_QUERY_RESOURCE = "/util/queryParser";
    private static final String QUERY_PARAM = "query";

    @Autowired private MockMvc mockMvc;

    @Test
    void validQueryRequest() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(PARSE_QUERY_RESOURCE)
                                .param(QUERY_PARAM, "accession:P21802")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("termQuery")))
                .andExpect(jsonPath("$.field", is("accession")))
                .andExpect(jsonPath("$.value", is("P21802")));
    }

    @Test
    void requiredQueryRequest() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(get(PARSE_QUERY_RESOURCE).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages.*", contains("query is a required parameter")));
    }

    @Test
    void invalidQueryRequest() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(PARSE_QUERY_RESOURCE)
                                .param(QUERY_PARAM, "length:[1 TO ]")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }
}
