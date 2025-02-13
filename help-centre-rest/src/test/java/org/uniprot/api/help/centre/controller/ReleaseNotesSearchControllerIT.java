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
 * @author jluo
 * @date: 13 Apr 2022
 */
@ContextConfiguration(classes = {HelpCentreRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ReleaseNotesController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ReleaseNotesSearchControllerIT.ReleaseNotesSearchContentTypeParamResolver.class,
            ReleaseNotesSearchControllerIT.ReleaseNotesSearchParameterResolver.class
        })
public class ReleaseNotesSearchControllerIT extends AbstractSearchWithFacetControllerIT {

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
        return "/release-notes/search";
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
                                "$.results[0].matches.title",
                                contains("title-<span class=\"match-highlight\">value</span>-2")))
                .andExpect(
                        jsonPath(
                                "$.results[0].matches.content",
                                contains(
                                        "content-<span class=\"match-highlight\">value</span>-clean 2")));
    }

    @Test
    void searchReturnsTitleFirst() throws Exception {
        saveEntry("1", "something else", "title is lovely", "category", "releaseNotes");
        saveEntry("2", "title is lovely", "content", "category", "releaseNotes");

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
    void searchReturnsNewestFirst() throws Exception {
        saveEntry("First", 1, "same title", "same content", "category", "releaseNotes");
        saveEntry("Fourth", 4, "same title", "same content", "category", "releaseNotes");
        saveEntry("Third", 3, "same title", "same content", "category", "releaseNotes");
        saveEntry("Second", 2, "same title", "same content", "category", "releaseNotes");
        saveEntry("Not Wanted", 2, "not the same", "same content", "category", "releaseNotes");

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
                .andExpect(jsonPath("$.results.size()", is(4)))
                .andExpect(
                        jsonPath("$.results.*.id", contains("Fourth", "Third", "Second", "First")));
    }

    @Test
    void canFindPartialMatchInTitle() throws Exception {
        saveEntry(
                "1",
                "something else",
                "content contains a word in title",
                "category",
                "releaseNotes");
        saveEntry("2", "title is lovely", "content", "category", "releaseNotes");

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
        saveEntry("1", "something else", "content has a word in title", "category", "releaseNotes");
        saveEntry("2", "title is lovely", "content", "category", "releaseNotes");

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
    void suggestionNotGivenIfNotRequired() throws Exception {
        saveEntry("1", "title", "content", "category", "releaseNotes");
        saveEntry("2", "ball", "content", "category", "releaseNotes");
        saveEntry("3", "bell", "content", "category", "releaseNotes");

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
    void canFindPartialWorldExactResultFirst() throws Exception {
        saveEntry("id0", "another fluffy protein", "content 0", "category", "releaseNotes");
        saveEntry("id00", "another one fluffy protein", "content 00", "category", "releaseNotes");
        saveEntry("id1", "goat cabbage protein ball", "content 1", "category", "releaseNotes");
        saveEntry("id2", "goat cabbage protein with ball", "content 2", "category", "releaseNotes");

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

    @Test
    void singleSuggestionGivenForMultiWordQuery() throws Exception {
        saveEntry("id1", "title", "content 1", "category", "releaseNotes");
        saveEntry("id2", "protein ball", "content 2", "category", "releaseNotes");
        saveEntry("id3", "protein ball", "content 3", "category", "releaseNotes");
        saveEntry("id4", "protein bill", "content 4", "category", "releaseNotes");

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

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    static class ReleaseNotesSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:id-value-10"))
                    .resultMatcher(jsonPath("$.results.size()", is(1)))
                    .resultMatcher(jsonPath("$.results[0].id", is("id-value-10")))
                    .resultMatcher(jsonPath("$.results[0].title", is("title-value-10")))
                    .resultMatcher(jsonPath("$.results[0].lastModified", is("2021-07-10")))
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

    static class ReleaseNotesSearchContentTypeParamResolver
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
                        .title("title-value-" + i)
                        .type(ReleaseNotesController.RELEASE_NOTES_STR)
                        .content("content-value-clean " + i)
                        .contentOriginal("content-value-original " + i)
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .releaseDate(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .categories(
                                List.of("category-value", "category-value-" + i, "releaseNotes"))
                        .build();
        getStoreManager().saveDocs(getStoreType(), doc);
    }

    private void saveEntry(String id, String title, String content, String... categories) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id(id)
                        .title(title)
                        .type(ReleaseNotesController.RELEASE_NOTES_STR)
                        .content(content)
                        .contentOriginal(content + "-original")
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, 1).getTime())
                        .categories(List.of(categories))
                        .build();

        getStoreManager().saveDocs(getStoreType(), doc);
    }

    private void saveEntry(
            String id, int dayOfMonth, String title, String content, String... categories) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id(id)
                        .title(title)
                        .type(ReleaseNotesController.RELEASE_NOTES_STR)
                        .content(content)
                        .contentOriginal(content + "-original")
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, 1).getTime())
                        .releaseDate(
                                new GregorianCalendar(1981, Calendar.NOVEMBER, dayOfMonth)
                                        .getTime())
                        .categories(List.of(categories))
                        .build();

        getStoreManager().saveDocs(getStoreType(), doc);
    }
}
