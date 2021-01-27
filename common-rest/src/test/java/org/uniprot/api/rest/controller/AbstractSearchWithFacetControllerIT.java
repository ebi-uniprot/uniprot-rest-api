package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.controller.param.SearchParameter;

public abstract class AbstractSearchWithFacetControllerIT extends AbstractSearchControllerIT {

    // ----------------------------------------- TEST FACETS
    // -----------------------------------------------
    @Test
    void searchFacetsWithCorrectValuesReturnSuccess(SearchParameter queryParameter)
            throws Exception {
        checkSearchParameterInput(queryParameter);
        assertThat(queryParameter.getQueryParams().keySet(), hasItems("facets", "query"));

        // given
        saveEntry(SaveScenario.FACETS_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach(
                        (paramName, values) ->
                                requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = getMockMvc().perform(requestBuilder);

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
    void searchFacetsWithIncorrectValuesReturnBadRequest() throws Exception {

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "*:*")
                                        .param("facets", "invalid, invalid2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        startsWith(
                                                "Invalid facet name 'invalid'. Expected value can be "),
                                        startsWith(
                                                "Invalid facet name 'invalid2'. Expected value can be "))));
    }

    @ParameterizedTest(name = "[{index}] search with facetName {0}")
    @MethodSource("getAllFacetFieldsArguments")
    void searchCanSearchWithAllAvailableFacetsFields(String facetField) throws Exception {
        // given
        saveEntry(SaveScenario.FACETS_SUCCESS);
        assertThat(facetField, notNullValue());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "*:*")
                                        .param("size", "0")
                                        .param("facets", facetField)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets.*.name", contains(facetField)));
    }

    protected abstract List<String> getAllFacetFields();

    private Stream<Arguments> getAllFacetFieldsArguments() {
        return getAllFacetFields().stream().map(Arguments::of);
    }
}
