package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.*;

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
class UniParcGetByDBRefIdIT extends AbstractGetMultipleUniParcByIdTest {
    private static final String getByDBIdPath = "/uniparc/dbreference/{dbid}";

    @Override
    protected String getGetByIdEndpoint() {
        return getByDBIdPath;
    }

    @Override
    protected String getSearchValue() {
        return "embl1";
    }

    @Test
    void testGetByDbIdSuccess() throws Exception {
        // when
        String dbid = "embl1";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083C01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)))
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
                        jsonPath("$.results[0].uniParcCrossReferences[*].taxonomy", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[0].proteinName",
                                notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].geneName", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].proteomeId",
                                notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.value", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.length", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.crc64", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.md5", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12)))
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
    void testGetByAccessionSuccess() throws Exception {
        // when
        String dbid = ACCESSION;
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)));
    }

    @Test
    void testGetByNonExistingDbIdSuccess() throws Exception {
        // when
        String dbid = "randomId";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", empty()));
    }

    @Test
    void testGetByDbIdWithDBFilterSuccess() throws Exception {
        // when
        String dbid = "embl1";
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083C01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].taxonomy", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12)));
    }

    @Test
    void testGetByDbIdWithInvalidDBFilterSuccess() throws Exception {
        // when
        String dbid = "unimes1";
        String dbTypes = "randomDB";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
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
    void testGetByDbIdWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String dbid = "embl1";
        String taxonIds = "9606,5555";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("taxonIds", taxonIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[0].organism.taxonId",
                                is(9606)));
    }

    @Test
    void testGetByDbIdWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String dbid = "unimes1";
        String active = "true";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(5)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(ACCESSION)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].taxonomy", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].active",
                                contains(true, true, true, true, true)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12)));
    }

    @Test
    void testGetByDbIdWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String dbid = "embl1";
        String active = "false";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo(UNIPARC_ID)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12)));
    }

    @Test
    void testGetByDbIdWithPagination() throws Exception {
        // when
        String dbid = "common-vector";
        int size = 2;
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
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
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)));

        String cursor1 = extractCursor(response);
        // when get second page
        ResultActions responsePage2 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
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
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)));

        String cursor2 = extractCursor(responsePage2);

        // when get third page
        ResultActions responsePage3 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
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
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(6)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)));
    }
}
