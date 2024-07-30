package org.uniprot.api.rest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.request.IdsSearchRequest;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

/**
 * @author sahmad
 * @created 27/06/2021
 */
public abstract class AbstractGetByIdsPostControllerIT extends AbstractStreamControllerIT {
    @Autowired private ObjectMapper objectMapper;

    @Test
    void getByIdsPostSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonString(getIdsDownloadRequest())));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
        verifyResults(response);
    }

    @Test
    void getByIdsFieldsParameterSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonString(getIdsDownloadWithFieldsRequest())));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));

        for (ResultMatcher matcher : getFieldsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void allContentTypeWorks(MediaType mediaType) throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header(ACCEPT, mediaType)
                                        .content(getJsonString(getIdsDownloadRequest())));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));

        if (!mediaType.equals(XLS_MEDIA_TYPE)) { // unable to compare xls binary type
            verifyIds(response);
        }
    }

    @Test
    void getByIdsBadRequestFailure() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonString(getInvalidDownloadRequest())));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(getErrorMessages())));
    }

    protected abstract IdsSearchRequest getInvalidDownloadRequest();

    @Test
    void getByIdsPostWithNullBodyFailure() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(post(getGetByIdsPath()).contentType(MediaType.APPLICATION_JSON));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder("request body is missing")));
    }

    @Test
    void getByIdsPostWithEmptyBodyFailure() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(getErrorMessage())));
    }

    @Test
    void getByIdsWithPassedFacetsIgnoredPostSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonRequestBodyWithFacets()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsWithPassedFacetFilterIgnoredPostSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonRequestBodyWithFacetFilter()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsDownloadFalseIgnored() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                post(getGetByIdsPath())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(getJsonRequestBodyWithDownloadParam()));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    protected abstract String getJsonRequestBodyWithDownloadParam();

    protected abstract String[] getErrorMessage();

    protected abstract String getJsonRequestBodyWithFacets();

    protected abstract String getJsonRequestBodyWithFacetFilter();

    protected abstract IdsSearchRequest getIdsDownloadRequest();

    protected abstract IdsSearchRequest getIdsDownloadWithFieldsRequest();

    protected abstract String getCommaSeparatedIds();

    protected abstract List<ResultMatcher> getResultsResultMatchers();

    protected abstract List<ResultMatcher> getIdsAsResultMatchers();

    protected abstract MockMvc getMockMvc();

    protected abstract String getGetByIdsPath();

    protected abstract String getCommaSeparatedReturnFields();

    protected abstract List<ResultMatcher> getFieldsResultMatchers();

    protected abstract String[] getErrorMessages();

    protected String getJsonString(IdsSearchRequest idsSearchRequest) {
        try {
            return this.objectMapper.writeValueAsString(idsSearchRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyResults(ResultActions response) throws Exception {
        for (ResultMatcher matcher : getResultsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    private void verifyIds(ResultActions response) throws Exception {
        for (ResultMatcher matcher : getIdsAsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(getGetByIdsPath());
    }
}
