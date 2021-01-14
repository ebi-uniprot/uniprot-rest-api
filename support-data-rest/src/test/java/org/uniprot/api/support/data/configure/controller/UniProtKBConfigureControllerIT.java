package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.greaterThan;
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
@WebMvcTest(UniProtKBConfigureController.class)
class UniProtKBConfigureControllerIT {

    private static final String BASIC_RESOURCE = "/configure/uniprotkb";

    @Autowired private MockMvc mockMvc;

    @Test
    void canGetUniProtSearchTermsTemp() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/search_terms")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetUniProtSearchTerms() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/search-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetAnnotationEvidences() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/annotation_evidences")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetGoEvidences() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/go_evidences")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetDatabases() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/databases").header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetAllDatabases() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/allDatabases")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetEvidenceDatabases() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/evidenceDatabases")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetResultFields() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/resultfields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetResultFieldsByNewAPI() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/result-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    private void validateResponse(ResultActions response) throws Exception {
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }
}
