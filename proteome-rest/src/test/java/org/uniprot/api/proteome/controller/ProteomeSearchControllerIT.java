package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.uniprot.api.proteome.controller.ProteomeControllerITUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

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
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.proteome.*;
import org.uniprot.core.proteome.impl.*;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 13 Jun 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ProteomeController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ProteomeSearchControllerIT.ProteomeSearchContentTypeParamResolver.class,
            ProteomeSearchControllerIT.ProteomeSearchParameterResolver.class
        })
class ProteomeSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static final String EXCLUDED_PROTEOME = "UP999999999";
    @Autowired private ProteomeQueryRepository repository;

    @Autowired private ProteomeFacetConfig facetConfig;

    @Value("${search.default.page.size}")
    protected String defaultPageSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.PROTEOME;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.proteome;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/proteomes/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return Integer.parseInt(defaultPageSize);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.PROTEOME;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "upid":
                value = UPID_PREF + 231;
                break;
            case "organism_id":
            case "taxonomy_id":
                value = "9606";
                break;

            case "organism_name":
                value = "human";
                break;
            case "annotation_score":
                value = "15";
                break;
            case "proteome_type":
                value = "4";
                break;
            case "busco":
                value = "[0 TO *]";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(231);
        saveEntry(520);
        saveExcluded();
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        saveExcluded();
    }

    private void saveEntry(int i) {
        ProteomeDocument document = getProteomeDocument(i);

        getStoreManager().saveDocs(DataStoreManager.StoreType.PROTEOME, document);
    }

    private void saveExcluded() {
        ProteomeDocument excludedProteomeDoc = getExcludedProteomeDocument(EXCLUDED_PROTEOME);
        getStoreManager().saveDocs(DataStoreManager.StoreType.PROTEOME, excludedProteomeDoc);
    }

    @Test
    void excludedIdReturnEmptyResult() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getSearchRequestPath())
                                .param("query","upid:"+EXCLUDED_PROTEOME)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    static class ProteomeSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:UP000005231"))
                    .resultMatcher(jsonPath("$.results.*.id", contains("UP000005231")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:UP000004231"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:*"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("organism_name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'organism_name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "upid:INVALID OR organism_id:INVALID "
                                            + "OR organism_name:INVALID OR taxonomy_id:invalid OR superkingdom:invalid"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID",
                                            "The organism id filter value should be a number",
                                            "The taxonomy id filter value should be a number")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("organism_name desc"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("organism"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .resultMatcher(jsonPath("$.results.*.taxonomy.taxonId", contains(9606, 9606)))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("superkingdom,proteome_type"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .resultMatcher(jsonPath("$.facets", iterableWithSize(2)))
                    .resultMatcher(jsonPath("$.facets[0].values", iterableWithSize(1)))
                    .resultMatcher(jsonPath("$.facets[0].label", is("Superkingdom")))
                    .resultMatcher(jsonPath("$.facets[0].name", is("superkingdom")))
                    .resultMatcher(jsonPath("$.facets[0].allowMultipleSelection", is(false)))
                    .resultMatcher(jsonPath("$.facets[0].values.*.value", contains("Eukaryota")))
                    .resultMatcher(jsonPath("$.facets[0].values.*.count", contains(2)))
                    .build();
        }
    }

    static class ProteomeSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("organism_id:9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    contains("UP000005231", "UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString("UP000005231")))
                                    .resultMatcher(content().string(containsString("UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("UP000005231")))
                                    .resultMatcher(content().string(containsString("UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Proteome Id\tOrganism\tOrganism Id\tProtein count")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UP000005231\tHomo sapiens\t9606\t21")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UP000005520\tHomo sapiens\t9606\t21")))
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
                    .query("upid:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
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
