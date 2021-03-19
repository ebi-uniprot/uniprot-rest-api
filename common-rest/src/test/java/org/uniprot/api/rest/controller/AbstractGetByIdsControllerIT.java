package org.uniprot.api.rest.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

/**
 * @author sahmad
 * @created 19/03/2021
 */
public abstract class AbstractGetByIdsControllerIT extends AbstractStreamControllerIT {
    protected abstract String getCommaSeparatedIds();

    protected abstract String getCommaSeparatedMixedIds();

    protected abstract String getCommaSeparatedMissingIds();

    protected abstract String getCommaSeparatedFacets();

    protected abstract List<ResultMatcher> getResultsResultMatchers();

    protected abstract List<ResultMatcher> getFacetsResultMatchers();

    protected abstract List<ResultMatcher> getDownloadResultMatchers();

    protected abstract MockMvc getMockMvc();

    protected abstract String getGetByIdsPath();

    protected abstract String getRequestParamName();

    protected abstract String getCommaSeparatedReturnFields();

    protected abstract List<ResultMatcher> getFieldsResultMatchers();

    protected abstract List<ResultMatcher> getFirstPageResultMatchers();

    protected abstract List<ResultMatcher> getSecondPageResultMatchers();

    protected abstract List<ResultMatcher> getThirdPageResultMatchers();

    protected abstract String[] getErrorMessages();

    @Test
    void getByIdsSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
        verifyResults(response);
    }

    @Test
    void getByIdsPostSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        post(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
        verifyResults(response);
    }

    @Test
    void getByIdsWithAllFacetsSuccess() throws Exception {
        String facets = getCommaSeparatedFacets();
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("facets", facets)
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));

        verifyResults(response);
        verifyFacets(response);
    }

    @Test
    void getByIdsWithAllFacetsOnlySuccess() throws Exception {
        String facets = getCommaSeparatedFacets();
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("facets", facets)
                                .param("size", "0"));
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));

        verifyFacets(response);
    }

    @Test
    void getByIdsDownloadWorks() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "true")
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                startsWith(
                                        "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(jsonPath("$.facets").doesNotExist());//no facets in download

        verifyResults(response);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void allContentTypeWorks(MediaType mediaType) throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, mediaType)
                                .param(getRequestParamName(), getCommaSeparatedIds()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));

        if (!mediaType.equals(XLS_MEDIA_TYPE)) { // unable to compare xls binary type
            verifyIds(response);
        }
    }

    @Test
    void getByIdsFieldsParameterWorks() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", getCommaSeparatedReturnFields())
                                .param(getRequestParamName(), getCommaSeparatedIds())
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));

        for (ResultMatcher matcher : getFieldsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    @Test
    void getByAccessionsWithPagination() throws Exception {
        int pageSize = 4;
        String facetList = getCommaSeparatedFacets();
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", getCommaSeparatedReturnFields())
                                .param("facets", facetList)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=4")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)));
        // verify first page results and facets
        for (ResultMatcher matcher : getFirstPageResultMatchers()) {
            response.andExpect(matcher);
        }
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[3].split("=")[1];
        // when 2nd page
        response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("fields", getCommaSeparatedReturnFields())
                                .param("facets", facetList)
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then 2nd page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=4")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)))
                .andExpect(jsonPath("$.facets").doesNotExist());
        for (ResultMatcher matcher : getSecondPageResultMatchers()) {
            response.andExpect(matcher);
        }

        linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        cursor = linkHeader.split("\\?")[1].split("&")[3].split("=")[1];

        // when last page
        response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds())
                                .param("fields", getCommaSeparatedReturnFields())
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then last page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.facets").doesNotExist());

        for (ResultMatcher matcher : getThirdPageResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    @Test
    void getByIdsWithFewIdsMissingFromStoreWithFacetsSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedMixedIds())
                                .param("facets", getCommaSeparatedFacets())
                                .param("size", "8"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(jsonPath("$.facets").exists());
    }

    @Test
    void getByIdsWithAllIdsMissingFromStoreWithFacetsSuccess() throws Exception {
        String facetList = getCommaSeparatedFacets();
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(getRequestParamName(), getCommaSeparatedMissingIds())
                                .param("facets", facetList)
                                .param("size", "2"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsBadRequest() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getGetByIdsPath())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "INVALID")
                                .param("fields", "invalid, invalid1")
                                .param(
                                        getRequestParamName(),
                                        getCommaSeparatedIds() + ",INVALID , INVALID2")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(getErrorMessages())));
    }

    private void verifyResults(ResultActions response) throws Exception {
        for (ResultMatcher matcher : getResultsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    private void verifyFacets(ResultActions response) throws Exception {
        for (ResultMatcher matcher : getFacetsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    private void verifyIds(ResultActions response) throws Exception {
        for (ResultMatcher matcher : getDownloadResultMatchers()) {
            response.andExpect(matcher);
        }
    }
}
