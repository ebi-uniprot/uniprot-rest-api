package org.uniprot.api.support.data.keyword.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.LongStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.core.cv.go.impl.GoTermBuilder;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
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
public class KeywordSearchControllerIT extends AbstractSearchControllerIT {

    @Autowired private KeywordRepository repository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

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
        return "/keyword/search";
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
        KeywordId keyword =
                new KeywordIdBuilder().name("my keyword " + keywordId).id(keywordId).build();
        KeywordId category = new KeywordIdBuilder().name("Ligand").id("KW-9993").build();

        KeywordEntry keywordEntry =
                new KeywordEntryBuilder()
                        .definition("Definition value")
                        .keyword(keyword)
                        .category(category)
                        .synonymsAdd("synonyms")
                        .parentsAdd(new KeywordEntryBuilder().keyword(keyword).build())
                        .childrenAdd(new KeywordEntryBuilder().keyword(keyword).build())
                        .geneOntologiesAdd(
                                new GoTermBuilder().id("idValue").name("nameValue").build())
                        .sitesAdd("siteValue")
                        .statistics(new StatisticsBuilder().build())
                        .build();

        KeywordDocument document =
                KeywordDocument.builder()
                        .id(keywordId)
                        .name("my keyword " + keywordId)
                        .definition("my definition " + keywordId)
                        .ancestor(Collections.singletonList("ancestor"))
                        .parent(Collections.singletonList("parent"))
                        .synonyms(Collections.singletonList("content"))
                        .keywordObj(getKeywordBinary(keywordEntry))
                        .build();

        getStoreManager().saveDocs(DataStoreManager.StoreType.KEYWORD, document);
    }

    private ByteBuffer getKeywordBinary(KeywordEntry entry) {
        try {
            return ByteBuffer.wrap(
                    KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse KeywordEntry to binary json: ", e);
        }
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
            return SearchParameter.builder().build();
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
                                                                    "Keyword ID\tName\tDescription\tCategory")))
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
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
