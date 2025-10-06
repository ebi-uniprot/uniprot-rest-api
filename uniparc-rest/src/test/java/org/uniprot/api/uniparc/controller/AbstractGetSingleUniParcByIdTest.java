package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author sahmad
 * @created 17/08/2020
 */
abstract class AbstractGetSingleUniParcByIdTest extends BaseUniParcGetByIdControllerTest {

    @Test
    void testGetByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(8)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].database",
                                containsInAnyOrder(
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL",
                                        "UniProtKB/TrEMBL",
                                        "EMBL")))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(14)));
    }

    @Test
    void testGetByAccessionWithInvalidDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "randomDB";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "is an invalid UniParc cross reference database name")));
    }

    @Test
    void testGetByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String taxonIds = "9606,5555";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("taxonIds", taxonIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism.taxonId", hasItem(9606)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(14)));
    }

    @Test
    void testGetByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "true";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(21)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", everyItem(is(true))))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(14)));
    }

    @Test
    void testGetByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "false";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", everyItem(is(false))))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(14)));
    }
}
