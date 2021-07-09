package org.uniprot.api.help.centre.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
            HelpCentreSearchControllerIT.HelpCentreSearchContentTypeParamResolver.class,
            HelpCentreSearchControllerIT.HelpCentreSearchParameterResolver.class
        })
public class HelpCentreSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    @Autowired private HelpCentreQueryRepository repository;
    @Autowired private HelpCentreFacetConfig facetConfig;

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
        return "/helpcentre/search";
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

    private void saveEntry(int i) {
        HelpDocument doc =
                HelpDocument.builder()
                        .id("id-value-" + i)
                        .title("title-value-" + i)
                        .content("content-value " + i)
                        .categories(List.of("category-value", "category-value-" + i))
                        .build();
        getStoreManager().saveDocs(getStoreType(), doc);
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
                    .queryParam("query", Collections.singletonList("content:content-value"))
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
                            jsonPath("$.results.*.id",
                                    contains("id-value-10", "id-value-20", "id-value-30")))
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
                                                    contains("id-value-10", "id-value-20", "id-value-30")))
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
}
