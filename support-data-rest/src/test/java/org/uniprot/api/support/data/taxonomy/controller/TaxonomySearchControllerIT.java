package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
import org.uniprot.api.rest.respository.taxonomy.TaxonomyRepository;
import org.uniprot.api.rest.request.taxonomy.TaxonomyFacetConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            TaxonomySearchControllerIT.TaxonomySearchContentTypeParamResolver.class,
            TaxonomySearchControllerIT.TaxonomySearchParameterResolver.class
        })
public class TaxonomySearchControllerIT extends AbstractSearchWithFacetControllerIT {

    @Autowired private TaxonomyFacetConfig facetConfig;

    @Autowired private TaxonomyRepository repository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.TAXONOMY;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.taxonomy;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/taxonomy/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.TAXONOMY;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "id":
            case "tax_id":
            case "host":
                value = "10";
                break;
            case "parent":
                value = "9";
                break;
            case "ancestor":
                value = "11";
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
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i, true));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10, true);
        saveEntry(15, true);
        saveEntry(20, false);
    }

    private void saveEntry(long taxId, boolean facet) {
        TaxonomyDocument document = TaxonomyITUtils.createSolrDoc(taxId, facet);
        getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    @Test
    void defaultSearchCanFindOtherName() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when accession field returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=otherName")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results[0].taxonId", is(10)))
                .andExpect(jsonPath("$.results[1].taxonId", is(15)));
    }

    static class TaxonomySearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("tax_id:" + 10))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10)))
                    .resultMatcher(jsonPath("$.results.*.scientificName", contains("scientific10")))
                    .resultMatcher(jsonPath("$.results.*.commonName", contains("common10")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("tax_id:9999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("scientific:*"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 15)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific10", "scientific15")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common15")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("scientific:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'scientific' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "tax_id:INVALID OR id:INVALID "
                                            + "OR host:INVALID OR linked:invalid OR ancestor:invalid"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The taxonomy ancestor filter value should be a number",
                                            "The taxonomy id filter value should be a number",
                                            "The taxonomy linked filter value should be a boolean",
                                            "The taxonomy id filter value should be a number",
                                            "The taxonomy host filter value should be a number")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("scientific desc"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(15, 10)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific15", "scientific10")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common15", "common10")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("common_name,mnemonic"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 15)))
                    .resultMatcher(jsonPath("$.results.*.scientificName").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common15")))
                    .resultMatcher(
                            jsonPath("$.results.*.mnemonic", contains("mnemonic10", "mnemonic15")))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("superkingdom,taxonomies_with"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 15)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific10", "scientific15")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common15")))
                    .resultMatcher(
                            jsonPath("$.results.*.mnemonic", contains("mnemonic10", "mnemonic15")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets.*.name", contains("superkingdom", "taxonomies_with")))
                    .resultMatcher(
                            jsonPath("$.facets.*.label", contains("Superkingdom", "Taxons with")))
                    .resultMatcher(jsonPath("$.facets[1].values.size()", is(5)))
                    .resultMatcher(jsonPath("$.facets[1].values[0].count", is(2)))
                    .resultMatcher(jsonPath("$.facets[1].values[0].value", is("1_uniprotkb")))
                    .resultMatcher(jsonPath("$.facets[1].values[0].label", is("UniProtKB entries")))
                    .resultMatcher(jsonPath("$.facets[1].values[1].value", is("2_reviewed")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[1].values[1].label",
                                    is("Reviewed (Swiss-Prot) entries")))
                    .resultMatcher(jsonPath("$.facets[1].values[2].value", is("3_unreviewed")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[1].values[2].label",
                                    is("Unreviewed (TrEMBL) entries")))
                    .resultMatcher(jsonPath("$.facets[1].values[3].value", is("4_reference")))
                    .resultMatcher(
                            jsonPath("$.facets[1].values[3].label", is("Reference proteomes")))
                    .resultMatcher(jsonPath("$.facets[1].values[4].value", is("5_proteome")))
                    .resultMatcher(jsonPath("$.facets[1].values[4].label", is("Proteomes")))
                    .build();
        }
    }

    static class TaxonomySearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("tax_id:10 OR tax_id:15 OR tax_id:20")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath("$.results.*.taxonId", contains(10, 15)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.scientificName",
                                                    contains("scientific10", "scientific15")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.commonName",
                                                    contains("common10", "common15")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("10")))
                                    .resultMatcher(content().string(containsString("15")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Taxon Id\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed\tRank\tLineage\tParent\tVirus hosts")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "10\tmnemonic10\tscientific10\tcommon10\tother names10\t\tfamily\t\t9")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "15\tmnemonic15\tscientific15\tcommon15\tother names15\t\tfamily\t\t14")))
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
                    .query("tax_id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The taxonomy id filter value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe taxonomy id filter value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe taxonomy id filter value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }
    }
}
