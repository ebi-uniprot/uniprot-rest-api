package org.uniprot.api.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.controller.param.SearchParameter;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public abstract class AbstractSearchWithFacetControllerIT extends AbstractSearchControllerIT {

    //----------------------------------------- TEST FACETS -----------------------------------------------
    @Test
    void searchFacetsWithCorrectValuesReturnSuccess(SearchParameter queryParameter) throws Exception {
        checkSearchParameterInput(queryParameter);
        assertThat(queryParameter.getQueryParams().keySet(), hasItems("facets", "query"));

        // given
        saveEntry(SaveScenario.FACETS_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder = get(getSearchRequestPath())
                .header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter.getQueryParams().forEach((paramName, values) -> requestBuilder.param(paramName, values.toArray(new String[0])));

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        ResultActions resultActions = response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchFacetsWithIncorrectValuesReturnBadRequest() throws Exception {

        // when
        ResultActions response = getMockMvc().perform(
                get(getSearchRequestPath())
                        .param("query", "*:*")
                        .param("facets", "invalid, invalid2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(
                        startsWith("Invalid facet name 'invalid'. Expected value can be "),
                        startsWith("Invalid facet name 'invalid2'. Expected value can be "))));
    }

    @Test
    void searchCanSearchWithAllAvailableFacetsFields() throws Exception {

        // given
        saveEntry(SaveScenario.FACETS_SUCCESS);

        List<String> facetFields = getAllFacetFields();
        assertThat(facetFields, notNullValue());
        assertThat(facetFields, not(emptyIterable()));

        for (String facetField : facetFields) {
            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query", "*:*")
                            .param("facets", facetField)
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", greaterThan(0)))
                    .andExpect(jsonPath("$.facets.size()", greaterThan(0)))
                    .andExpect(jsonPath("$.facets.*.name", contains(facetField)));
        }
    }
}
