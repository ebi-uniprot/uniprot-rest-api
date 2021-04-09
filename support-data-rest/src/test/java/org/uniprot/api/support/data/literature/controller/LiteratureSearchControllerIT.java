package org.uniprot.api.support.data.literature.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.citation.SubmissionDatabase;
import org.uniprot.core.citation.impl.*;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            LiteratureSearchControllerIT.LiteratureSearchContentTypeParamResolver.class,
            LiteratureSearchControllerIT.LiteratureSearchParameterResolver.class
        })
class LiteratureSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static final String SUBMISSION_ID = "CI-6LG40CJ34FGTT";

    @Autowired private LiteratureFacetConfig facetConfig;

    @Autowired private LiteratureRepository repository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.LITERATURE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.literature;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/citations/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.LITERATURE;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "id":
                value = "10";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i, i % 2 == 0));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10, true);
        saveEntry(20, false);

        Submission submission =
                new SubmissionBuilder()
                        .title("The Submission Title")
                        .authorsAdd(new AuthorBuilder("The Submission Author").build())
                        .publicationDate(new PublicationDateBuilder("2021").build())
                        .submittedToDatabase(SubmissionDatabase.PDB)
                        .build();

        LiteratureEntry literatureEntry = new LiteratureEntryBuilder().citation(submission).build();

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(SUBMISSION_ID)
                        .literatureObj(LiteratureITUtils.getLiteratureBinary(literatureEntry))
                        .build();
        this.getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    @Test
    void validSubmissionSearch() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getSearchRequestPath())
                        .param("query", "id:" + SUBMISSION_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].citation.id", is(SUBMISSION_ID)))
                .andExpect(
                        jsonPath(
                                "$.results[0].citation.authors", contains("The Submission Author")))
                .andExpect(jsonPath("$.results[0].citation.title", is("The Submission Title")));
    }

    private void saveEntry(long pubMedId, boolean facet) {

        LiteratureDocument document = LiteratureITUtils.createSolrDoc(pubMedId, facet);

        getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    static class LiteratureSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:10"))
                    .resultMatcher(jsonPath("$.results.*.citation.id", contains("10")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.citationCrossReferences[0].id",
                                    contains("10")))
                    .resultMatcher(jsonPath("$.results.*.citation.title", contains("title 10")))
                    .resultMatcher(
                            jsonPath("$.results.*.citation.publicationDate", contains("2019")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("title:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.id", contains("10", "20", SUBMISSION_ID)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.citationCrossReferences[0].id",
                                    containsInAnyOrder("10", "20")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.title",
                                    containsInAnyOrder(
                                            "title 10", "title 20", "The Submission Title")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.publicationDate",
                                    containsInAnyOrder("2019", "2019", "2021")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("title:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'title' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The citation id value should be a PubMedId (number) or start with CI- or start with IND")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("title desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.citationCrossReferences[0].id",
                                    contains("20", "10")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.id", contains("20", "10", SUBMISSION_ID)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.title",
                                    contains("title 20", "title 10", "The Submission Title")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,title"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.id", contains("10", "20", SUBMISSION_ID)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.title",
                                    contains("title 10", "title 20", "The Submission Title")))
                    .resultMatcher(jsonPath("$.results.*.citation.authors").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.citation.journal").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("citations_with"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.citationCrossReferences[0].id",
                                    contains("10", "20")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.id", contains("10", "20", SUBMISSION_ID)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.citation.title",
                                    contains("title 10", "title 20", "The Submission Title")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets.size()", is(1)))
                    .resultMatcher(jsonPath("$.facets[0].name", is("citations_with")))
                    .resultMatcher(jsonPath("$.facets[0].values.size()", is(5)))
                    .resultMatcher(jsonPath("$.facets[0].values[0].label", is("UniProtKB entries")))
                    .resultMatcher(jsonPath("$.facets[0].values[0].value", is("1_uniprotkb")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values[1].label",
                                    is("UniProtKB reviewed entries")))
                    .resultMatcher(jsonPath("$.facets[0].values[1].value", is("2_reviewed")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values[2].label",
                                    is("UniProtKB unreviewed entries")))
                    .resultMatcher(jsonPath("$.facets[0].values[2].value", is("3_unreviewed")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values[3].label",
                                    is("Computationally mapped entries")))
                    .resultMatcher(jsonPath("$.facets[0].values[3].value", is("4_computationally")))
                    .resultMatcher(
                            jsonPath("$.facets[0].values[4].label", is("Community mapped entries")))
                    .resultMatcher(jsonPath("$.facets[0].values[4].value", is("5_community")))
                    .build();
        }
    }

    static class LiteratureSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:10 OR id:20")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.citation.citationCrossReferences[0].id",
                                                    containsInAnyOrder("10", "20")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.citation.title",
                                                    containsInAnyOrder("title 10", "title 20")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.citation.publicationDate",
                                                    containsInAnyOrder("2019", "2019")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("10")))
                                    .resultMatcher(content().string(containsString("20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Citation Id\tTitle\tReference\tAbstract/Summary")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "10\ttitle 10\tjournal 10 volume value:firstPage value-lastPage value(2019)\tliteratureAbstract value")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "20\ttitle 20\tjournal 20 volume value:firstPage value-lastPage value(2019)\tliteratureAbstract value")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The citation id value should be a PubMedId (number) or start with CI- or start with IND")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
