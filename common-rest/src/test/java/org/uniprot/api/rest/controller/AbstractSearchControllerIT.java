package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.ControllerITUtils.CACHE_VALUE;
import static org.uniprot.api.rest.controller.ControllerITUtils.verifyContentTypes;
import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RESULTS;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSearchControllerIT {

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @AfterEach
    void cleanData() {
        storeManager.cleanSolr(getStoreType());
    }

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        ReflectionTestUtils.setField(
                getRepository(), "solrClient", storeManager.getSolrClient(getStoreType()));
    }

    @Test
    void searchCanReturnSuccess(SearchParameter queryParameter) throws Exception {
        checkSearchParameterInput(queryParameter);
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, CACHE_VALUE))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(
                                header().stringValues(
                                                HttpHeaders.VARY,
                                                ACCEPT,
                                                ACCEPT_ENCODING,
                                                HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                                HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchCanReturnNotFound(SearchParameter queryParameter) throws Exception {
        checkSearchParameterInput(queryParameter);

        saveEntry(SaveScenario.SEARCH_NOT_FOUND);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, CACHE_VALUE))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(header().string(X_TOTAL_RESULTS, "0"))
                        .andExpect(
                                header().stringValues(
                                                HttpHeaders.VARY,
                                                ACCEPT,
                                                ACCEPT_ENCODING,
                                                HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                                HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    // ----------------------------------------- TEST QUERY
    // -----------------------------------------------
    @Test
    void searchWithoutQueryReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(get(getSearchRequestPath()).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("'query' is a required parameter")));
    }

    @Test
    void searchAllowQueryAllDocumentsReturnSuccess() throws Exception {
        // given
        saveEntry(SaveScenario.ALLOW_QUERY_ALL);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "*:*")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @Test
    void searchDefaultQueryReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "defaultQuery")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    // Forward slashes were causing errors during parsing of query in {@link QueryProcessor}.
    // This test checks that this does not happen.
    @Test
    void searchDefaultQueryWithForwardSlashesReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param(
                                        "query",
                                        "(6-phosphofructo-2-kinase/fructose-2,6-bisphosphatase 4)")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                // We are not interested in the actual results returned, we just want to make
                // sure this query does not cause an error. (The search behaviour is instead
                // delegated to store's integration-test module.)
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    @Test
    void searchQueryWithInvalidQueryFormatReturnBadRequest() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "invalidfield(:invalidValue AND :invalid:10)")
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
    void searchQueryWithInvalidQueryFormatMultipleMiddleWildcardReturnBadRequest()
            throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "pro*te*in")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "We only allow one wildcard character (*) in the middle of a search term. "
                                                + "Please check the help page for more information using wildcards for searches.")));
    }

    @Test
    void searchQueryWithInvalidFieldNameReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param(
                                        "query",
                                        "invalidfield:invalidValue OR invalidfield2:invalidValue2")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalidfield' is not a valid search field",
                                        "'invalidfield2' is not a valid search field")));
    }

    @ParameterizedTest(name = "[{index}] search fieldName {0}")
    @MethodSource("getAllSearchFields")
    void searchCanSearchWithAllSearchFields(String searchField) throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_ALL_FIELDS);

        assertThat(searchField, notNullValue());

        // when
        String fieldValue = getFieldValueForField(searchField);
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", searchField + ":" + fieldValue)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @Test
    void searchAllowWildcardQueryAllDocuments(SearchParameter queryParameter) throws Exception {
        checkSearchParameterInput(queryParameter);
        assertThat(queryParameter.getQueryParams().keySet(), hasItem("query"));

        // given
        saveEntry(SaveScenario.ALLOW_WILDCARD_QUERY);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach((paramName, values) -> requestBuilder.param(paramName, "*"));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchQueryWithInvalidTypeQueryReturnBadRequest(SearchParameter queryParameter)
            throws Exception {
        checkSearchParameterInput(queryParameter);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchQueryWithInvalidValueQueryReturnBadRequest(SearchParameter queryParameter)
            throws Exception {
        checkSearchParameterInput(queryParameter);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    // ----------------------------------------- TEST SORTS
    // -----------------------------------------------
    @Test
    void searchSortWithCorrectValuesReturnSuccess(SearchParameter queryParameter) throws Exception {
        checkSearchParameterInput(queryParameter);
        assertThat(queryParameter.getQueryParams().keySet(), hasItems("sort", "query"));

        // given
        saveEntry(SaveScenario.SORT_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchSortWithIncorrectValuesReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "*:*")
                                .param("sort", "invalidField desc,invalidField1 invalidSort1")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid sort field order 'invalidsort1'. Expected asc or desc",
                                        "Invalid sort field 'invalidfield1'",
                                        "Invalid sort field 'invalidfield'")));
    }

    @ParameterizedTest(name = "[{index}] sort {0}")
    @MethodSource("getAllSortFields")
    void searchCanSearchWithAllAvailableSortFields(String sortField) throws Exception {
        // given
        saveEntry(SaveScenario.SORT_SUCCESS);

        assertThat(sortField, notNullValue());

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "*:*")
                                .param("sort", sortField + " asc")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    // ----------------------------------------- TEST RETURNED FIELDS
    // -----------------------------------------------
    @Test
    void searchFieldsWithCorrectValuesReturnSuccess(SearchParameter queryParameter)
            throws Exception {
        checkSearchParameterInput(queryParameter);
        assertThat(queryParameter.getQueryParams().keySet(), hasItems("fields", "query"));

        // given
        saveEntry(SaveScenario.FIELDS_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchFieldsWithIncorrectValuesReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "*:*")
                                .param("fields", "invalidField, otherInvalid")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalidField'",
                                        "Invalid fields parameter value 'otherInvalid'")));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void searchCanSearchWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_ALL_RETURN_FIELDS);

        assertThat(name, notNullValue());
        assertThat(paths, notNullValue());
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", getAllReturnedFieldsQuery())
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)));

        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*]." + path;
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    // ----------------------------------------- TEST CONTENT TYPES
    // -----------------------------------------------
    @Test
    void searchSuccessContentTypes(SearchContentTypeParam contentTypeParam) throws Exception {
        checkSearchContentTypeParameterInput(contentTypeParam);

        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        assertThat(contentTypeParam, notNullValue());
        assertThat(contentTypeParam.getContentTypeParams(), notNullValue());
        assertThat(contentTypeParam.getContentTypeParams(), not(empty()));

        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getSearchRequestPath())
                            .param("query", contentTypeParam.getQuery())
                            .header(ACCEPT, contentType.getContentType());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.OK.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    contentType.getContentType().toString()));
            for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        }
    }

    @Test
    void searchBadRequestContentTypes(SearchContentTypeParam contentTypeParam) throws Exception {
        checkSearchContentTypeParameterInput(contentTypeParam);

        // when
        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getSearchRequestPath())
                            .param("query", contentTypeParam.getQuery())
                            .header(ACCEPT, contentType.getContentType());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    contentType.getContentType().toString()));

            for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        }
    }

    private void checkSearchContentTypeParameterInput(SearchContentTypeParam contentTypeParam) {
        assertThat(contentTypeParam, notNullValue());
        assertThat(contentTypeParam.getQuery(), not(isEmptyOrNullString()));
        assertThat(contentTypeParam.getContentTypeParams(), notNullValue());
        assertThat(contentTypeParam.getContentTypeParams(), not(emptyIterable()));
        verifyContentTypes(
                getSearchRequestPath(),
                requestMappingHandlerMapping,
                contentTypeParam.getContentTypeParams());
    }

    // -----------------------------------------------
    // TEST DEFAULT CONTENT TYPE AND FORMAT
    // -----------------------------------------------

    // if no content type is provided, use json
    @Test
    void searchWithoutContentTypeMeansUseDefaultContentType() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response = mockMvc.perform(get(getSearchRequestPath()).param("query", "*:*"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, DEFAULT_MEDIA_TYPE_VALUE));
    }

    // if format parameter for content type present for search, use it
    @Test
    void searchWithFormatParameterMeansUseThatContentType() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        String extension = "json";
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "*:*")
                                .param("format", extension));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.getMediaTypeForFileExtension(extension)
                                                .toString()));
    }

    // ----------------------------------------- TEST PAGINATION
    // -----------------------------------------------

    @Test
    void searchWithInvalidPageSizeZeroReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "-1"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @Test
    void searchWithInvalidPageSizeBiggerThanMaxReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "" + (SearchRequest.MAX_RESULTS_SIZE + 1)));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    @Test
    void searchWithoutPageSizeReturnDefaultPageSize() throws Exception {
        // given
        int savedEntries = getDefaultPageSize() + 1;
        saveEntries(savedEntries);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(savedEntries)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        header().string(
                                        HttpHeaders.LINK,
                                        containsString("size=" + getDefaultPageSize())))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(getDefaultPageSize())));
    }

    @Test
    void searchWithPageSize5() throws Exception {
        // given
        saveEntries(5);

        // when page
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "5"));

        // then page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(5)));
    }

    @Test
    void searchSizeBiggerThanDefaultPageSize() throws Exception {
        // given
        saveEntries(getDefaultPageSize() + 10);

        // when page
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "" + (getDefaultPageSize() + 1)));

        // then page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "" + (getDefaultPageSize() + 10)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(jsonPath("$.results.size()", is(getDefaultPageSize() + 1)));
    }

    @Test
    void searchCanPaginateOverTwoPagesResults() throws Exception {
        // given
        saveEntries(6);

        // when first page
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "5"));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "6"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(5)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());

        String cursor = linkHeader.split("\\?")[1].split("&")[1].split("=")[1];
        // when last page
        response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("cursor", cursor)
                                .param("size", "5"));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "6"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)));
    }

    @Test
    void searchWithLeadingWildcardIgnoredWarningSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*valuerandom"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.warnings.length()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.warnings[0].message",
                                is(
                                        "Leading wildcard (*, ?) was removed for this search. Please check the help page for more information on using wildcards on queries.")))
                .andExpect(jsonPath("$.warnings[0].code", is(41)));
    }

    protected DataStoreManager getStoreManager() {
        return storeManager;
    }

    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    protected abstract DataStoreManager.StoreType getStoreType();

    protected abstract SolrCollection getSolrCollection();

    protected abstract SolrQueryRepository getRepository();

    protected abstract String getSearchRequestPath();

    protected abstract int getDefaultPageSize();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract void saveEntry(SaveScenario saveContext);

    protected abstract void saveEntries(int numberOfEntries);

    protected abstract String getFieldValueForValidatedField(String searchField);

    protected String getAllReturnedFieldsQuery() {
        return "*:*";
    }

    private Stream<Arguments> getAllSearchFields() {
        Stream<Arguments> fields =
                getSearchFieldConfig().getSearchFieldItems().stream()
                        .map(SearchFieldItem::getFieldName)
                        .map(Arguments::of);
        Stream<Arguments> aliases =
                getSearchFieldConfig().getSearchFieldItems().stream()
                        .map(SearchFieldItem::getAliases)
                        .flatMap(List::stream)
                        .map(Arguments::of);
        return Stream.concat(fields, aliases);
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType())
                .getReturnFields()
                .stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig = getSearchFieldConfig();
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    boolean fieldValueIsValid(String field, String value) {
        return getSearchFieldConfig().isSearchFieldValueValid(field, value);
    }

    private SearchFieldConfig getSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType());
    }

    private String getFieldValueForField(String searchField) {
        String value = getFieldValueForValidatedField(searchField);
        if (value.isEmpty()) {
            if (fieldValueIsValid(searchField, "*")) {
                value = "*";
            } else if (fieldValueIsValid(searchField, "true")) {
                value = "true";
            }
        }
        return value;
    }

    void checkSearchParameterInput(SearchParameter queryParameter) {
        assertThat(queryParameter, notNullValue());
        assertThat(queryParameter.getQueryParams(), notNullValue());
        assertThat(queryParameter.getQueryParams().keySet(), not(emptyIterable()));
        assertThat(queryParameter.getResultMatchers(), notNullValue());
        assertThat(queryParameter.getResultMatchers(), not(emptyIterable()));
    }
}
