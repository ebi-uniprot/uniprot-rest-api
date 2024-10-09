package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RdfPrologs;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 15/06/2020
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcProteomeStreamControllerIT extends UniParcStreamControllerIT {
    private static final String streamByProteomeIdRequestPath = "/uniparc/proteome/{upId}/stream";
    private static final String UP_ID = "UP000005640";

    @Test
    void streamByProteomeIdRdfCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RdfPrologs.UNIPARC_PROLOG)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n\n"
                                                        + "</rdf:RDF>")));
    }

    @Test
    void streamByProteomeIdCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    void streamByProteomeIdBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(streamByProteomeIdRequestPath, UP_ID)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "invalid,invalid1")
                                .param("sort", "invalid")
                                .param("download", "invalid"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "Invalid sort parameter format. Expected format fieldName asc|desc.",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    void streamByProteomeIdyPDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniparc_")))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamByProteomeIdSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("sort", "upi desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    void streamByProteomeIdDefaultSearchWithLowerCaseId() throws Exception {

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID.toLowerCase())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @ParameterizedTest(name = "[{index}] sort fieldName {0}")
    @MethodSource("getAllSortFields")
    void streamByProteomeIdCanSortAllPossibleSortFields(String sortField) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("sort", sortField + " asc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamByProteomeIdFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "gene,organism_id");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                hasItems("UPI0000283A01", "UPI0000283A02")))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcCrossReferences.*.geneName",
                                hasItems("geneName01", "geneName02")))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcCrossReferences.*.organism.taxonId",
                                hasItems(9606, 7787, 9606, 7787)))
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.sequenceFeatures").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void streamByProteomeIdAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID).header(ACCEPT, mediaType);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }
}
