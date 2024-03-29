package org.uniprot.api.help.centre.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.help.centre.HelpCentreRestApplication;
import org.uniprot.api.help.centre.repository.HelpCentreFacetConfig;
import org.uniprot.api.help.centre.repository.HelpCentreQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 09/07/2021
 */
@ContextConfiguration(classes = {HelpCentreRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(HelpCentreController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            HelpCentreSearchControllerIT.HelpCentreSearchContentTypeParamResolver.class,
            HelpCentreSearchControllerIT.HelpCentreSearchParameterResolver.class
        })
class HelpCentreSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    @Autowired private HelpCentreQueryRepository repository;
    @Autowired private HelpCentreFacetConfig facetConfig;
    @Autowired private MockMvc mockMvc;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.HELP;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.help;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/help/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.HELP;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "*";
        if ("release_date".equals(searchField)) {
            value = "[* TO *]";
        }
        return value;
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10);
        saveEntry(20);
        saveEntry(30);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    @Override
    protected String getAllReturnedFieldsQuery() {
        return "content:content";
    }

    @Test
    void searchReturnCorrectMatchedResults() throws Exception {
        saveEntries(2);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "value")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.id", contains("id-value-2", "id-value-1")))
                .andExpect(
                        jsonPath(
                                "$.results[0].matches.content",
                                contains(
                                        "content-<span class=\"match-highlight\">value</span>-clean 2")))
                .andExpect(
                        jsonPath(
                                "$.results[0].matches.title",
                                contains("title-<span class=\"match-highlight\">value</span>-2")));
    }

    @Test
    void searchReturnsTitleFirst() throws Exception {
        saveEntry("1", "something else", "title is lovely", "category", "help");
        saveEntry("2", "title is lovely", "content", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "title")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.id", contains("2", "1")));
    }

    @Test
    void canFindPartialMatchInTitle() throws Exception {
        saveEntry("1", "something else", "content contains a word in title", "category", "help");
        saveEntry("2", "title is lovely", "content", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "som")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.id", contains("1")));
    }

    @Test
    void canFindPartialMatchesInContent() throws Exception {
        saveEntry("1", "something else", "content has a word in title", "category", "help");
        saveEntry("2", "title is lovely", "content", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "con")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.id", contains("2", "1")));
    }

    @Test
    void suggestionGivenForSingleWordQuery() throws Exception {
        saveEntry("id1", "title", "content 1", "help");
        saveEntry("id2", "ball", "content 2", "help");
        saveEntry("id3", "ball", "content 3", "help");
        saveEntry("id4", "bill", "content 4", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "bell")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.suggestions.size()", is(2)))
                .andExpect(jsonPath("$.suggestions[0].query", is("ball")))
                .andExpect(jsonPath("$.suggestions[0].hits", is(2)))
                .andExpect(jsonPath("$.suggestions[1].query", is("bill")))
                .andExpect(jsonPath("$.suggestions[1].hits", is(1)));
    }

    @Test
    void singleSuggestionGivenForMultiWordQuery() throws Exception {
        saveEntry("id1", "title", "content 1", "category", "help");
        saveEntry("id2", "protein ball", "content 2", "category", "help");
        saveEntry("id3", "protein ball", "content 3", "category", "help");
        saveEntry("id4", "protein bill", "content 4", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "\"protein bell\"")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.suggestions.size()", is(2)))
                .andExpect(jsonPath("$.suggestions[0].query", is("\"protein ball\"")))
                .andExpect(jsonPath("$.suggestions[0].hits", is(2)))
                .andExpect(jsonPath("$.suggestions[1].query", is("\"protein bill\"")))
                .andExpect(jsonPath("$.suggestions[1].hits", is(1)));
    }

    @Test
    void multipleSuggestionsGivenForMultiWordQuery() throws Exception {
        saveEntry("id0", "another fluffy protein bill", "content 0", "category", "help");
        saveEntry("id00", "another one fluffy protein bill", "content 00", "category", "help");
        saveEntry("id1", "title", "content 1", "category", "help");
        saveEntry("id2", "goat cabbage protein ball", "content 2", "category", "help");
        saveEntry("id3", "rigorous protein ball", "content 3", "category", "help");
        saveEntry("id4", "pink fluffy protein bill", "content 4", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "protin bell")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.suggestions.size()", is(2)))
                .andExpect(jsonPath("$.suggestions[0].query", is("protein bill")))
                .andExpect(jsonPath("$.suggestions[0].hits", is(3)))
                .andExpect(jsonPath("$.suggestions[1].query", is("protein ball")))
                .andExpect(jsonPath("$.suggestions[1].hits", is(2)));
    }

    @Test
    void suggestionNotGivenIfNotRequired() throws Exception {
        saveEntry("1", "title", "content", "category", "help");
        saveEntry("2", "ball", "content", "category", "help");
        saveEntry("3", "bell", "content", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "bell")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.id", contains("3")))
                .andExpect(jsonPath("$.suggestions").doesNotExist());
    }

    @Test
    void multipleSuggestionsGivenForPhraseQuery() throws Exception {
        saveEntry("id0", "another fluffy protein bill", "content 0", "category", "help");
        saveEntry("id00", "another one fluffy protein bill", "content 00", "category", "help");
        saveEntry("id1", "title", "content 1", "category", "help");
        saveEntry("id2", "goat cabbage protein ball", "content 2", "category", "help");
        saveEntry("id3", "rigorous protein ball", "content 3", "category", "help");
        saveEntry("id4", "pink fluffy protein bill", "content 4", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "\"protin bell\"")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.suggestions.size()", is(2)))
                .andExpect(jsonPath("$.suggestions[0].query", is("\"protein bill\"")))
                .andExpect(jsonPath("$.suggestions[0].hits", is(3)))
                .andExpect(jsonPath("$.suggestions[1].query", is("\"protein ball\"")))
                .andExpect(jsonPath("$.suggestions[1].hits", is(2)));
    }

    @Test
    void canFindPartialWorldExactResultFirst() throws Exception {
        saveEntry("id0", "another fluffy protein", "content 0", "category", "help");
        saveEntry("id00", "another one fluffy protein", "content 00", "category", "help");
        saveEntry("id1", "goat cabbage protein ball", "content 1", "category", "help");
        saveEntry("id2", "goat cabbage protein with ball", "content 2", "category", "help");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(getSearchRequestPath())
                                .param("query", "protein b")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.id", contains("id1", "id2")));
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    static class HelpCentreSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:id-value-10"))
                    .resultMatcher(jsonPath("$.results.size()", is(1)))
                    .resultMatcher(jsonPath("$.results[0].id", is("id-value-10")))
                    .resultMatcher(jsonPath("$.results[0].title", is("title-value-10")))
                    .resultMatcher(jsonPath("$.results[0].lastModified", is("2021-07-10")))
                    .resultMatcher(jsonPath("$.results[0].type", is("help")))
                    .resultMatcher(jsonPath("$.results[0].releaseDate", is("2021-07-10")))
                    .resultMatcher(jsonPath("$.results[0].content").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:id-value-not-found"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("id-value-30", "id-value-20", "id-value-10")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'id' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:99999"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("The 'id' is invalid. It can not be a number.")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("title desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("id-value-30", "id-value-20", "id-value-10")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("title:title-value"))
                    .queryParam("fields", Collections.singletonList("id"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("id-value-30", "id-value-20", "id-value-10")))
                    .resultMatcher(jsonPath("$.results.*.title").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .queryParam("facets", Collections.singletonList("category"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("id-value-30", "id-value-20", "id-value-10")))
                    .resultMatcher(jsonPath("$.facets.*.label", contains("Category")))
                    .resultMatcher(jsonPath("$.facets[0].values.size()", greaterThan(3)))
                    .resultMatcher(
                            jsonPath("$.facets[0].values.*.value", hasItem("category-value")))
                    .resultMatcher(jsonPath("$.facets[0].values.*.count", hasItem(3)))
                    .build();
        }
    }

    static class HelpCentreSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("*")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    contains(
                                                            "id-value-30",
                                                            "id-value-20",
                                                            "id-value-10")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:9999")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'id' is invalid. It can not be a number.")))
                                    .build())
                    .build();
        }
    }

    private void saveEntry(int i) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id("id-value-" + i)
                        .type(HelpCentreController.HELP_STR)
                        .title("title-value-" + i)
                        .content("content-value-clean " + i)
                        .contentOriginal("content-value-original " + i)
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .categories(List.of("category-value", "category-value-" + i, "help"))
                        .releaseDate(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .build();
        getStoreManager().saveDocs(getStoreType(), doc);
    }

    private void saveEntry(String id, String title, String content, String... categories) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id(id)
                        .title(title)
                        .type(HelpCentreController.HELP_STR)
                        .content(content)
                        .contentOriginal(content + "-original")
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, 1).getTime())
                        .categories(List.of(categories))
                        .build();

        getStoreManager().saveDocs(getStoreType(), doc);
    }
}
