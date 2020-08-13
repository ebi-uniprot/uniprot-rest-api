package org.uniprot.api.uniparc.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.uniparc.UniParcRestApplication;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author sahmad
 * @since 2020-08-11
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetByUpIdIT extends AbstractGetByIdTest{


    private static final String getByUpIdPath = "/uniparc/proteome/{upid}";
    @Override
    protected String getGetByIdEndpoint() {
        return getByUpIdPath;
    }

    @Test
    void testGetByUpIdSuccess() throws Exception {
        // when
        String upid = "UP123456701";
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].version", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].versionI", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].active", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].created", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].lastUpdated",
                                notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[0].properties",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties[*].key",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties[*].value",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties[*].value",
                                hasItem(upid)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.value", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.length", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.crc64", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.md5", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].databaseId", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].locations", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].sequenceFeatures[0].locations", iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].locations[*].start",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].locations[*].end",
                                notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].sequenceFeatures[*].interproGroup", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].interproGroup.id",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].interproGroup.name",
                                notNullValue()))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[0].taxonomies[*].scientificName", notNullValue()))
                .andExpect(jsonPath("$.results[0].taxonomies[*].taxonId", notNullValue()));
    }
}
