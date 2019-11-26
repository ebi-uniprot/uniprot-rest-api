package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
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
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/** @author lgonzales */
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
        SolrTemplate template = new SolrTemplate(getStoreManager().getSolrClient(getStoreType()));
        template.afterPropertiesSet();
        ReflectionTestUtils.setField(getRepository(), "solrTemplate", template);
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
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

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
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

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
        response.andDo(print())
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
        response.andDo(print())
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalidfield' is not a valid search field",
                                        "'invalidfield2' is not a valid search field")));
    }

    @Test
    void searchCanSearchWithAllSearchFields() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_ALL_FIELDS);

        Collection<String> searchFields = getAllSearchFields();
        assertThat(searchFields, notNullValue());
        assertThat(searchFields, not(emptyIterable()));

        for (String searchField : searchFields) {
            // when
            String fieldValue = getFieldValueForField(searchField);
            ResultActions response =
                    mockMvc.perform(
                            get(getSearchRequestPath())
                                    .param("query", searchField + ":" + fieldValue)
                                    .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", greaterThan(0)));
        }
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
                response.andDo(print())
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
                response.andDo(print())
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
                response.andDo(print())
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
                response.andDo(print())
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
        response.andDo(print())
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

    @Test
    void searchCanSearchWithAllAvailableSortFields() throws Exception {
        // given
        saveEntry(SaveScenario.SORT_SUCCESS);

        Collection<String> sortFields = getAllSortFields();
        assertThat(sortFields, notNullValue());
        assertThat(sortFields, not(emptyIterable()));

        for (String sortField : sortFields) {
            // when
            ResultActions response =
                    mockMvc.perform(
                            get(getSearchRequestPath())
                                    .param("query", "*:*")
                                    .param("sort", sortField + " asc")
                                    .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", greaterThan(0)));
        }
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
                response.andDo(print())
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalidField'",
                                        "Invalid fields parameter value 'otherInvalid'")));
    }

    @Disabled // FIXME
    @Test
    void searchCanSearchWithAllAvailableReturnedFields() throws Exception {

        // given
        saveEntry(SaveScenario.SEARCH_ALL_RETURN_FIELDS);

        List<String> returnFields = getAllReturnedFields();
        assertThat(returnFields, notNullValue());
        assertThat(returnFields, not(emptyIterable()));

        for (String returnField : returnFields) {
            // when
            ResultActions response =
                    mockMvc.perform(
                            get(getSearchRequestPath())
                                    .param("query", "*:*")
                                    .param("fields", returnField)
                                    .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", greaterThan(0)));
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
                    response.andDo(print())
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
                    response.andDo(print())
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
        ControllerITUtils.verifyContentTypes(
                getSearchRequestPath(),
                requestMappingHandlerMapping,
                contentTypeParam.getContentTypeParams());
    }

    // ----------------------------------------- TEST PAGINATION
    // -----------------------------------------------

    @Test
    void searchWithInvalidPageSizeReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "0"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("'size' must be greater than 0")));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", String.valueOf(savedEntries)))
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "5"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(5)));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "6"))
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "6"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)));
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

    protected abstract Collection<String> getAllSearchFields();

    protected abstract String getFieldValueForValidatedField(String searchField);

    protected abstract Collection<String> getAllSortFields();

    protected abstract List<String> getAllFacetFields();

    protected abstract List<String> getAllReturnedFields();

    protected abstract void saveEntry(SaveScenario saveContext);

    protected abstract void saveEntries(int numberOfEntries);

    protected abstract boolean fieldValueIsValid(String field, String value);

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
