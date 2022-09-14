package org.uniprot.api.rest.controller;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author sahmad
 * @created 02/03/2022
 */
public abstract class AbstractSearchWithSuggestionsControllerIT
        extends AbstractSearchWithFacetControllerIT {

    @ParameterizedTest(name = "{0} misspelt `{1}`")
    @MethodSource(value = "provideMisspeltSearchString")
    void testGetSuggestionForSearchWithTypo(
            String field, String searchString, List<String> alternatives) throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_NOT_FOUND);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=" + searchString)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", is(empty())))
                .andExpect(jsonPath("$.suggestions.size()", is(alternatives.size())))
                .andExpect(jsonPath("$.suggestions[0].query", is(alternatives.get(0))))
                .andExpect(jsonPath("$.suggestions[0].hits", notNullValue()));
    }

    @Test
    void testGetSuggestionWithFieldAndValueSearchWithTypo() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_NOT_FOUND);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=taxonomy_name:homan")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", is(empty())))
                .andExpect(jsonPath("$.suggestions.size()", greaterThan(0)))
                .andExpect(jsonPath("$.suggestions[0].query", is("taxonomy_name:human")))
                .andExpect(jsonPath("$.suggestions[0].hits", greaterThan(0)))
                .andExpect(jsonPath("$.suggestions[1].query", is("taxonomy_name:homo")))
                .andExpect(jsonPath("$.suggestions[1].hits", greaterThan(0)));
    }

    @Test
    void testGetNoSuggestionForSearch() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_NOT_FOUND);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=cambridge")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", is(empty())))
                .andExpect(jsonPath("$.suggestions").doesNotExist());
    }

    protected abstract List<Triple<String, String, List<String>>> getTriplets();

    private Stream<Arguments> provideMisspeltSearchString() {
        return getTriplets().stream().map(this::mapToArguments);
    }

    private Arguments mapToArguments(Triple<String, String, List<String>> triples) {
        return Arguments.of(triples.getLeft(), triples.getMiddle(), triples.getRight());
    }
}
