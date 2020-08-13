package org.uniprot.api.uniparc.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.core.util.Utils;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;

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
class UniParcGetByDbIdIT extends AbstractGetByIdTest{
    private static final String getByDBIdPath = "/uniparc/dbreference/{dbid}";
    @Override
    protected String getGetByIdEndpoint() {
        return getByDBIdPath;
    }

    @Test
    void testGetByDbIdSuccess() throws Exception {
        // when
        String dbid = "embl1";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)))
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

    @Test
    void testGetByAccessionSuccess() throws Exception {
        // when
        String dbid = "P12301";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)));
    }

    @Test
    void testGetByNonExistingDbIdSuccess() throws Exception {
        // when
        String dbid = "randomId";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid));

        // then
        response.andDo(print())
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
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
    void testGetByDbIdWithDBIdsFilterSuccess() throws Exception {
        // when
        String dbid = "unimes1";
        String dbIds = "P12301,P10001,randomId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].id",
                                containsInAnyOrder( "P10001", "P12301")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByDbIdWithMoreDBIdsThanSupportedFilterSuccess() throws Exception {
        // when
        String dbid = "unimes1";
        String dbIds = "dbId1,dbId2,dbId3,dbId4,dbId5,dbId6,dbId7";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
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
    void testGetByDbIdWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String dbid = "embl1";
        String taxonIds = "9606,radomTaxonId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), dbid)
                                .param("taxonIds", taxonIds));

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
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(dbid)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].taxonomies[0].taxonId", is(9606)));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(3)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].active",
                                contains(true, true, true)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testGetDbIdWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        String dbid = "embl1";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), dbid)
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)));

        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*]." + path;
            log.info("ReturnField:" + name + " Validation Path: " + returnFieldValidatePath);
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    @Test
    void testGetDbIdWithInvalidReturnedFields() throws Exception {
        String dbid = "P12301";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), dbid)
                                .param("fields", "randomField")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("Invalid fields parameter value ")));
    }

    @ParameterizedTest(name = "[{index}] get by dbId with content-type {0}")
    @ValueSource(
            strings = {
                    "",
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                    TSV_MEDIA_TYPE_VALUE,
                    XLS_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE
            })
    void testGetByDbIdSupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), "embl1")
                                .header(ACCEPT, contentType));

        if (Utils.nullOrEmpty(contentType)) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
    }

    @ParameterizedTest(name = "[{index}] try to get by dbid with content-type {0}")
    @ValueSource(
            strings = {MediaType.APPLICATION_PDF_VALUE, UniProtMediaType.OBO_MEDIA_TYPE_VALUE})
    void testGetByDbIdWithUnsupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), "P12301")
                                .header(ACCEPT, contentType));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted:")));
    }
}