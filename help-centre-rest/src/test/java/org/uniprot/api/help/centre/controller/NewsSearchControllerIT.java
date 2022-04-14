package org.uniprot.api.help.centre.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
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
@ContextConfiguration(
        classes = {
            HelpCentreStoreTestConfig.class,
            HelpCentreRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(HelpCentreController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            NewsSearchControllerIT.NewsSearchContentTypeParamResolver.class,
            NewsSearchControllerIT.NewsSearchParameterResolver.class
        })
public class NewsSearchControllerIT extends AbstractSearchWithFacetControllerIT {

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
        return "/news/search";
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
        return "";
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
                .andExpect(jsonPath("$.results.*.id", contains("id-value-1", "id-value-2")))
                .andExpect(
                        jsonPath(
                                "$.results[0].matches.title",
                                contains("title-<span class=\"match-highlight\">value</span>-1")))
                .andExpect(
                        jsonPath(
                                "$.results[0].matches.content",
                                contains(
                                        "content-<span class=\"match-highlight\">value</span>-clean 1")));
    }

    @Test
    void searchReturnsTitleFirst() throws Exception {
        saveEntry("1", "something else", "title is lovely", "category", "news");
        saveEntry("2", "title is lovely", "content", "category", "news");

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
        saveEntry("1", "something else", "content contains a word in title", "category", "news");
        saveEntry("2", "title is lovely", "content", "category", "news");

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
        saveEntry("1", "something else", "content has a word in title", "category", "news");
        saveEntry("2", "title is lovely", "content", "category", "news");

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
        saveEntry("1", "title", "content", "category", "news");
        saveEntry("2", "ball", "content", "category", "news");
        saveEntry("3", "bell", "content", "category", "news");

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
        saveEntry("id0", "another fluffy protein", "content 0", "category", "news");
        saveEntry("id00", "another one fluffy protein", "content 00", "category", "news");
        saveEntry("id1", "goat cabbage protein ball", "content 1", "category", "news");
        saveEntry("id2", "goat cabbage protein with ball", "content 2", "category", "news");

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

    static class NewsSearchParameterResolver extends AbstractSearchParameterResolver {

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
                                    contains("id-value-10", "id-value-20", "id-value-30")))
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
                                    contains("id-value-10", "id-value-20", "id-value-30")))
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
                                    contains("id-value-10", "id-value-20", "id-value-30")))
                    .resultMatcher(jsonPath("$.facets.*.label", contains("Category")))
                    .resultMatcher(jsonPath("$.facets[0].values.size()", greaterThan(3)))
                    .resultMatcher(
                            jsonPath("$.facets[0].values.*.value", hasItem("category-value")))
                    .resultMatcher(jsonPath("$.facets[0].values.*.count", hasItem(3)))
                    .build();
        }
    }

    static class NewsSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver {

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
                                                            "id-value-10",
                                                            "id-value-20",
                                                            "id-value-30")))
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
                        .type("releaseNote")
                        .content("content-value-clean " + i)
                        .contentOriginal("content-value-original " + i)
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .releaseDate(new GregorianCalendar(2021, Calendar.JULY, i).getTime())
                        .categories(List.of("category-value", "category-value-" + i, "news"))
                        .build();
        getStoreManager().saveDocs(getStoreType(), doc);
    }

    private void saveEntry(String id, String title, String content, String... categories) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id(id)
                        .title(title)
                        .content(content)
                        .contentOriginal(content + "-original")
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, 1).getTime())
                        .categories(List.of(categories))
                        .build();

        getStoreManager().saveDocs(getStoreType(), doc);
    }
}
