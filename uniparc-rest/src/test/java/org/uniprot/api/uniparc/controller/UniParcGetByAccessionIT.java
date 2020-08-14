package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.extern.slf4j.Slf4j;

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
class UniParcGetByAccessionIT extends AbstractGetByIdTest {

    private static final String getGetByIdEndpoint = "/uniparc/accession/{accession}";

    @Override
    protected String getGetByIdEndpoint() {
        return getGetByIdEndpoint;
    }

    @Override
    protected String getSearchValue() {
        return "P12301";
    }

    @Test
    void testGetByAccessionSuccess() throws Exception {
        // when
        String accession = "P12301";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(5)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].version", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].versionI", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].created", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].lastUpdated", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].properties", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].properties", iterableWithSize(4)))
                .andExpect(
                        jsonPath("$.uniParcCrossReferences[*].properties[*].key", notNullValue()))
                .andExpect(
                        jsonPath("$.uniParcCrossReferences[*].properties[*].value", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequence.value", notNullValue()))
                .andExpect(jsonPath("$.sequence.length", notNullValue()))
                .andExpect(jsonPath("$.sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("$.sequence.crc64", notNullValue()))
                .andExpect(jsonPath("$.sequence.md5", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.sequenceFeatures[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].databaseId", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[0].locations", iterableWithSize(2)))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations[*].start", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations[*].end", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup.id", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup.name", notNullValue()))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)))
                .andExpect(jsonPath("$.taxonomies[*].scientificName", notNullValue()))
                .andExpect(jsonPath("$.taxonomies[*].taxonId", notNullValue()));
    }

    @Test
    void testGetByNonExistingAccession() throws Exception {
        // when
        String accession = "P54321";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", hasItem("Resource not found")));
    }

    @Test
    void testGetByAccessionWithInvalidAccession() throws Exception {
        String accession = "ABCDEFG";
        // when
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                equalTo(
                                        "The 'accession' value has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    void testGetByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithInvalidDBFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbTypes = "randomDB";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("is invalid UniParc Cross Ref DB Name")));
    }

    @Test
    void testGetByAccessionWithDBIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbIds = "unimes1,P10001,randomId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].id",
                                containsInAnyOrder("unimes1", "P10001")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithMoreDBIdsThanSupportedFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbIds = "dbId1,dbId2,dbId3,dbId4,dbId5,dbId6,dbId7";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "is the maximum count limit of comma separated items. You have passed")));
    }

    @Test
    void testGetByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String taxonIds = "9606,radomTaxonId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("taxonIds", taxonIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(5)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(1)))
                .andExpect(jsonPath("$.taxonomies[0].taxonId", is(9606)));
    }

    @Test
    void testGetByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "true";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].active",
                                contains(true, true, true, true)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "false";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }
}
