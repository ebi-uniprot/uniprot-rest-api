package org.uniprot.api.literature;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.literature.repository.LiteratureRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.impl.AuthorImpl;
import org.uniprot.core.citation.impl.PublicationDateImpl;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureStoreEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.field.LiteratureField;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            LiteratureSearchControllerIT.LiteratureSearchContentTypeParamResolver.class,
            LiteratureSearchControllerIT.LiteratureSearchParameterResolver.class
        })
public class LiteratureSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    @Autowired private LiteratureFacetConfig facetConfig;

    @Autowired private LiteratureRepository repository;

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
        return "/literature/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected Collection<String> getAllSearchFields() {
        return Arrays.stream(LiteratureField.Search.values())
                .map(LiteratureField.Search::getName)
                .collect(Collectors.toList());
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
    protected List<String> getAllSortFields() {
        return Arrays.stream(LiteratureField.Sort.values())
                .map(LiteratureField.Sort::name)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(LiteratureField.ResultFields.values())
                .map(LiteratureField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean fieldValueIsValid(String field, String value) {
        return LiteratureField.Search.valueOf(field).hasValidValue(value);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i, i % 2 == 0));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10, true);
        saveEntry(20, false);
    }

    private void saveEntry(long pubMedId, boolean facet) {

        LiteratureEntry entry =
                new LiteratureEntryBuilder()
                        .pubmedId(pubMedId)
                        .doiId("doi " + pubMedId)
                        .title("title " + pubMedId)
                        .addAuthor(new AuthorImpl("author " + pubMedId))
                        .journal("journal " + pubMedId)
                        .publicationDate(new PublicationDateImpl("2019"))
                        .build();

        LiteratureStoreEntry storeEntry =
                new LiteratureStoreEntryBuilder().literatureEntry(entry).build();

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(pubMedId))
                        .doi(entry.getDoiId())
                        .title(entry.getTitle())
                        .author(
                                entry.getAuthors().stream()
                                        .map(Author::getValue)
                                        .collect(Collectors.toSet()))
                        .journal(entry.getJournal().getName())
                        .published(entry.getPublicationDate().getValue())
                        .citedin(facet)
                        .mappedin(facet)
                        .content(Collections.singleton(String.valueOf(pubMedId)))
                        .literatureObj(getLiteratureBinary(storeEntry))
                        .build();

        getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    private ByteBuffer getLiteratureBinary(LiteratureStoreEntry entry) {
        try {
            return ByteBuffer.wrap(
                    LiteratureJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }

    static class LiteratureSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:10"))
                    .resultMatcher(jsonPath("$.results.*.pubmedId", contains(10)))
                    .resultMatcher(jsonPath("$.results.*.title", contains("title 10")))
                    .resultMatcher(jsonPath("$.results.*.publicationDate", contains("2019")))
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
                    .resultMatcher(jsonPath("$.results.*.pubmedId", containsInAnyOrder(10, 20)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.title",
                                    containsInAnyOrder("title 10", "title 20")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.publicationDate",
                                    containsInAnyOrder("2019", "2019")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("title:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'title' filter type 'range' is invalid. Expected 'term' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "id:INVALID OR citedin:INVALID OR mappedin:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The PubMed id value should be a number",
                                            "The literature mappedin filter value should be a boolean",
                                            "The literature citedin filter value should be a boolean")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("title desc"))
                    .resultMatcher(jsonPath("$.results.*.pubmedId", contains(20, 10)))
                    .resultMatcher(jsonPath("$.results.*.title", contains("title 20", "title 10")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,title"))
                    .resultMatcher(jsonPath("$.results.*.pubmedId", contains(10, 20)))
                    .resultMatcher(jsonPath("$.results.*.title", contains("title 10", "title 20")))
                    .resultMatcher(jsonPath("$.results.*.authors").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.journal").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("citedin,mappedin"))
                    .resultMatcher(jsonPath("$.results.*.pubmedId", contains(10, 20)))
                    .resultMatcher(jsonPath("$.results.*.title", contains("title 10", "title 20")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name", contains("citedin", "mappedin")))
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
                                                    "$.results.*.pubmedId",
                                                    containsInAnyOrder(10, 20)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.title",
                                                    containsInAnyOrder("title 10", "title 20")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.publicationDate",
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
                                                                    "PubMed ID\tTitle\tReference\tAbstract/Summary")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "10\ttitle 10\tjournal 10:(2019)")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "20\ttitle 20\tjournal 20:(2019)")))
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
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The PubMed id value should be a number")))
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
