package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RESULTS;

import org.junit.jupiter.api.Disabled;
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
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;

import lombok.extern.slf4j.Slf4j;

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
@Disabled
class UniParcGetByProteomeIdIT extends AbstractGetMultipleUniParcByIdTest {

    private static final String getByUpIdPath = "/uniparc/proteome/{upid}";

    @Override
    protected String getGetByIdEndpoint() {
        return getByUpIdPath;
    }

    @Override
    protected String getSearchValue() {
        return "UP123456701";
    }

    @Test
    void testGetByUpIdSuccess() throws Exception {
        // when
        String upid = "UP123456701";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid)
                                .param("fields", "proteome,fullSequence,fullsequencefeatures"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.results[0].proteomes[*].id", hasItem(upid)))
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
                                notNullValue()));
    }

    @Test
    void testGetByProteomeIdWithPagination() throws Exception {
        // when
        String upid = "UP000005640";
        int size = 2;
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid)
                                .param("fields", "proteome")
                                .param("size", String.valueOf(size)));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains("UPI0000083C01", "UPI0000083C02")))
                .andExpect(jsonPath("$.results[0].proteomes[*].id", hasItem(upid)));

        String cursor1 = extractCursor(response);
        // when get second page
        ResultActions responsePage2 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid)
                                .param("fields", "proteome")
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor1));

        // then verify second page
        responsePage2
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains("UPI0000083C03", "UPI0000083C04")))
                .andExpect(jsonPath("$.results[0].proteomes[*].id", hasItem(upid)));

        String cursor2 = extractCursor(responsePage2);

        // when get third page
        ResultActions responsePage3 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid)
                                .param("fields", "proteome")
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor2));

        // then verify third page
        responsePage3
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.uniParcId", contains("UPI0000083C05")))
                .andExpect(jsonPath("$.results[0].proteomes[*].id", hasItem(upid)));
    }

    @Test
    void testGetByNonExistingProteomeIdSuccess() throws Exception {
        // when
        String upid = "randomId";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), upid));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", empty()));
    }
}
