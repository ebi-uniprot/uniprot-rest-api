package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RESULTS;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author sahmad
 * @created 19/03/2021
 */
public abstract class AbstractGetByIdsControllerIT extends AbstractStreamControllerIT {

    @Test
    void getByIdsSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("size", "10"));

        // then
        response.andDo(log())
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("facets", facets)
                                        .param("size", "10"));

        // then
        response.andDo(log())
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("facets", facets)
                                        .param("size", "0"));
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));

        verifyFacets(response);
    }

    @Test
    void getByIdsDownloadWorks() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("download", "true")
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\""
                                                        + getContentDisposition())))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(jsonPath("$.facets").doesNotExist()); // no facets in download

        verifyResults(response);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void allContentTypeWorks(MediaType mediaType) throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));

        for (ResultMatcher matcher : getFieldsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    @Test
    void getByIdsWithPagination() throws Exception {
        int pageSize = 4;
        String facetList = getCommaSeparatedFacets();
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param("facets", facetList)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "10"))
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param("facets", facetList)
                                        .param("cursor", cursor)
                                        .param("size", String.valueOf(pageSize)));

        // then 2nd page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "10"))
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param("cursor", cursor)
                                        .param("size", String.valueOf(pageSize)));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "10"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.facets").doesNotExist());

        for (ResultMatcher matcher : getThirdPageResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    @Test
    void getByIdsWithMixMissingIdsPagination() throws Exception {
        int pageSize = 4;
        String ids = getCommaSeparatedMixedIds();
        String[] idsArray = ids.split(",");
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param(getRequestParamName(), ids)
                                        .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "8"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=4")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(3)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[2].split("=")[1];
        // when last page
        response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                                        .param(getRequestParamName(), ids)
                                        .param("fields", getCommaSeparatedReturnFields())
                                        .param("cursor", cursor)
                                        .param("size", String.valueOf(pageSize)));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "8"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsWithFewIdsMissingFromStoreWithFacetsSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedMixedIds())
                                        .param("facets", getCommaSeparatedFacets())
                                        .param("size", "8"));

        // then
        response.andDo(log())
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
                getMockMvc()
                        .perform(
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
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("download", "INVALID")
                                        .param("fields", "invalid, invalid1")
                                        .param(
                                                getRequestParamName(),
                                                getCommaSeparatedIds() + ",INVALID , INVALID2")
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(getErrorMessages())));
    }

    @Test
    void getByIdsWithInvalidFacets() throws Exception {
        String facetList = "invalid_facet1";
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("facets", facetList)
                                        .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", containsInAnyOrder(getInvalidFacetErrorMessage())));
    }

    @Test
    void getByIdsWithPageSizeMoreThanIdsSize() throws Exception {
        int pageSize = 30;
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "10"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(10)));

        verifyResults(response);
    }

    @Test
    void getByIdsQueryFilterSuccess() throws Exception {
        String queryFilter = getQueryFilter();
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("query", queryFilter)
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(getSortedIdResultMatcher())
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsWithQueryFilterEmptyResponse() throws Exception {
        String queryFilter = getUnmatchedQueryFilter();
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("query", queryFilter)
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsWithFacetsAndQueryFilterSuccess() throws Exception {
        String queryFilter = getQueryFilter();
        int facetCount = getCommaSeparatedFacets().split(",").length;
        int idCount = 5;
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(
                                                getRequestParamName(),
                                                getCommaSeparatedNIds(idCount))
                                        .param("facets", getCommaSeparatedFacets())
                                        .param("query", queryFilter));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(idCount)))
                .andExpect(jsonPath("$.facets.size()", lessThanOrEqualTo(facetCount)));

        for (int i = 0;
                i < facetCount;
                i++) { // none of the facet count should exceed total number ids passed
            response.andExpect(
                    jsonPath(
                            "$.facets[" + i + "].values[?(@.count > " + idCount + ")]",
                            emptyIterable()));
            if (i < 9) // Fix test data to have all facets in UniProtKB
            response.andExpect(
                        jsonPath("$.facets[" + i + "].values[?(@.count <= " + idCount + ")]")
                                .isArray());
        }
    }

    @Test
    void getByIdsQueryFilterQuerySyntaxBadRequest() throws Exception {
        String queryFilter = "invalidfield(:invalidValue AND :invalid:10)";
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("query", queryFilter)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }

    @Test
    void getByTooManyIdsSuccess() throws Exception {
        // when
        String tooManyIds = (getCommaSeparatedIds() + ",").repeat(100);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), tooManyIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1000)));
    }

    @Test
    void getByMoreThanAllowedIdsFailure() throws Exception {
        // when
        String tooManyIds = (getCommaSeparatedIds() + ",").repeat(101);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), tooManyIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(getIdLengthErrorMessage())));
    }

    @ParameterizedTest(name = "[{index}] sort {0}")
    @MethodSource("getAllSortFields")
    void getByIdsWithAllAvailableSortFields(String sortField) throws Exception {
        // given
        assertThat(sortField, notNullValue());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("sort", sortField + " asc")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void getByIdsSortWithCorrectValuesSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("sort", getIdSortField() + " desc")
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(getReverseSortedIdResultMatcher())
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig = getSearchFieldConfig();
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private SearchFieldConfig getSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType());
    }

    protected abstract String getIdSortField();

    protected abstract String getCommaSeparatedIds();

    protected abstract String getCommaSeparatedNIds(int n);

    protected abstract String getCommaSeparatedMixedIds();

    protected abstract String getCommaSeparatedMissingIds();

    protected abstract String getCommaSeparatedFacets();

    protected abstract List<ResultMatcher> getResultsResultMatchers();

    protected abstract List<ResultMatcher> getFacetsResultMatchers();

    protected abstract List<ResultMatcher> getIdsAsResultMatchers();

    protected abstract MockMvc getMockMvc();

    protected abstract String getGetByIdsPath();

    protected abstract String getRequestParamName();

    protected abstract String getCommaSeparatedReturnFields();

    protected abstract List<ResultMatcher> getFieldsResultMatchers();

    protected abstract List<ResultMatcher> getFirstPageResultMatchers();

    protected abstract List<ResultMatcher> getSecondPageResultMatchers();

    protected abstract List<ResultMatcher> getThirdPageResultMatchers();

    protected abstract String[] getErrorMessages();

    protected abstract String[] getInvalidFacetErrorMessage();

    protected abstract String getQueryFilter();

    protected abstract ResultMatcher getSortedIdResultMatcher();

    protected abstract ResultMatcher getReverseSortedIdResultMatcher();

    protected abstract String getUnmatchedQueryFilter();

    protected abstract String[] getIdLengthErrorMessage();

    protected abstract UniProtDataType getUniProtDataType();

    public abstract String getContentDisposition();

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
        for (ResultMatcher matcher : getIdsAsResultMatchers()) {
            response.andExpect(matcher);
        }
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(getGetByIdsPath());
    }
}
