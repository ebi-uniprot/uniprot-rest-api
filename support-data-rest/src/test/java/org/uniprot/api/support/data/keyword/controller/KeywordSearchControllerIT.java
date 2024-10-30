package org.uniprot.api.support.data.keyword.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
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
import org.uniprot.api.support.data.common.keyword.repository.KeywordFacetConfig;
import org.uniprot.api.support.data.common.keyword.repository.KeywordRepository;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(KeywordController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            KeywordSearchControllerIT.KeywordSearchContentTypeParamResolver.class,
            KeywordSearchControllerIT.KeywordSearchParameterResolver.class
        })
class KeywordSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    @Autowired private KeywordRepository repository;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer solrBatchSize;

    @Autowired private KeywordFacetConfig facetConfig;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.KEYWORD;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.keyword;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/keywords/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.KEYWORD;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "id":
            case "keyword_id":
                value = "KW-0001";
                break;
        }
        return value;
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries)
                .forEach(i -> saveEntry("KW-000" + i, i % 2 == 0));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry("KW-0001", true);
        saveEntry("KW-0002", false);
    }

    private void saveEntry(String keywordId, boolean facet) {
        KeywordDocument document = KeywordITUtils.createSolrDocument(keywordId, facet);
        getStoreManager().saveDocs(DataStoreManager.StoreType.KEYWORD, document);
    }

    @Test
    void defaultSearchWithLowercaseId() throws Exception {
        // given
        saveEntries(2);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                getSearchRequestPath() + "?query=(kw-0001)")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results[0].keyword.id", is("KW-0001")));
    }

    private ByteBuffer getKeywordBinary(KeywordEntry entry) {
        try {
            return ByteBuffer.wrap(
                    KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse KeywordEntry to binary json: ", e);
        }
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    static class KeywordSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("keyword_id:KW-0001"))
                    .resultMatcher(jsonPath("$.results.*.keyword.id", contains("KW-0001")))
                    .resultMatcher(
                            jsonPath("$.results.*.keyword.name", contains("my keyword KW-0001")))
                    .resultMatcher(jsonPath("$.results.*.definition", contains("Definition value")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("keyword_id:KW-5555"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("name:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.keyword.id",
                                    containsInAnyOrder("KW-0001", "KW-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.keyword.name",
                                    containsInAnyOrder("my keyword KW-0001", "my keyword KW-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    containsInAnyOrder("Definition value", "Definition value")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query", Collections.singletonList("keyword_id:INVALID OR id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The keyword id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'",
                                            "The keyword keyword_id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("name desc"))
                    .resultMatcher(
                            jsonPath("$.results.*.keyword.id", contains("KW-0002", "KW-0001")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.keyword.name",
                                    contains("my keyword KW-0002", "my keyword KW-0001")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    contains("Definition value", "Definition value")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,name"))
                    .resultMatcher(
                            jsonPath("$.results.*.keyword.id", contains("KW-0001", "KW-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.keyword.name",
                                    contains("my keyword KW-0001", "my keyword KW-0002")))
                    .resultMatcher(jsonPath("$.results.*.definition").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.category").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", List.of("category"))
                    .resultMatcher(
                            jsonPath("$.results.*.keyword.id", contains("KW-0001", "KW-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.keyword.name",
                                    contains("my keyword KW-0001", "my keyword KW-0002")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name", contains("category")))
                    .resultMatcher(jsonPath("$.facets.*.label", contains("Category")))
                    .resultMatcher(jsonPath("$.facets[0].values.size()", is(1)))
                    .resultMatcher(jsonPath("$.facets[0].values.size()", is(1)))
                    .resultMatcher(jsonPath("$.facets[0].values[0].count", is(2)))
                    .resultMatcher(jsonPath("$.facets[0].values[0].value", is("ligand")))
                    .resultMatcher(jsonPath("$.facets[0].values[0].label", is("Ligand")))
                    .build();
        }
    }

    static class KeywordSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("keyword_id:KW-0001 OR keyword_id:KW-0002")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.keyword.id",
                                                    containsInAnyOrder("KW-0002", "KW-0001")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.keyword.name",
                                                    containsInAnyOrder(
                                                            "my keyword KW-0002",
                                                            "my keyword KW-0001")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.definition",
                                                    containsInAnyOrder(
                                                            "Definition value",
                                                            "Definition value")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("KW-0001")))
                                    .resultMatcher(content().string(containsString("KW-0002")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Keyword ID\tName\tDefinition\tCategory")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "KW-0001\tmy keyword KW-0001\tDefinition value\tLigand")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "KW-0002\tmy keyword KW-0002\tDefinition value\tLigand")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                    .resultMatcher(
                                            content().string(containsString("format-version: 1.2")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "default-namespace: uniprot:keywords")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "[Typedef]\n"
                                                                            + "id: category\n"
                                                                            + "name: category\n"
                                                                            + "is_cyclic: false")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "id: KW-0001\n"
                                                                            + "name: my keyword KW-0001\n"
                                                                            + "def: \"Definition value\" []\n"
                                                                            + "synonym: \"synonyms\" [UniProt]\n"
                                                                            + "xref: idValue \"nameValue\"\n"
                                                                            + "xref: linkValue\n"
                                                                            + "is_a: KW-0001\n"
                                                                            + "relationship: category KW-9993")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("keyword_id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The keyword keyword_id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe keyword keyword_id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe keyword keyword_id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    is(
                                                                            "Error messages\nThe keyword keyword_id filter value has invalid format. It should match the regular expression 'KW-[0-9]{4}'"))))
                                    .build())
                    .build();
        }
    }
}
