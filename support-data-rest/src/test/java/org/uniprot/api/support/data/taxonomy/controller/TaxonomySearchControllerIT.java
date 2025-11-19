package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.support.data.taxonomy.controller.TaxonomyITUtils.createInactiveTaxonomySolrDoc;
import static org.uniprot.api.support.data.taxonomy.controller.TaxonomyITUtils.createSolrDoc;

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
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyRepository;
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

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
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
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i, true, true));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10, true, true);
        saveEntry(15, true, false);
        saveEntry(20, true, true);
    }

    private void saveEntry(long taxId, boolean facet, boolean active) {
        TaxonomyDocument document =
                active ? createSolrDoc(taxId, facet) : createInactiveTaxonomySolrDoc(taxId);
        getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    @Test
    void testTaxonIdBoost() throws Exception {
        // given
        // create first active entry with taxon id 1000 but scientific name has 1001
        TaxonomyDocument document1 = createSolrDoc(1000, true);
        document1 = document1.toBuilder().scientific("scientific 1001 name").build();
        getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document1);
        // create second active entry with taxon id 1001
        TaxonomyDocument document2 = createSolrDoc(1001, true);
        document2 =
                document2.toBuilder()
                        .scientific("scientific name")
                        .common("common name")
                        .otherNames(List.of("other names"))
                        .mnemonic("mnemonic")
                        .build();
        getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document2);
        // 3rd active entry without 1001 anywhere
        TaxonomyDocument document3 = createSolrDoc(1002, true);
        getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document3);
        // when search by free text 1001
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=(1001)")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // verify result order, second entry with tax_id 1001 and then first entry with tax id 1000
        // in name
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results[0].taxonId", is(1001)))
                .andExpect(jsonPath("$.results[0].active", is(true)))
                .andExpect(jsonPath("$.results[1].taxonId", is(1000)))
                .andExpect(jsonPath("$.results[1].active", is(true)));
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
                .andExpect(jsonPath("$.results[0].active", is(true)))
                .andExpect(jsonPath("$.results[1].taxonId", is(20)))
                .andExpect(jsonPath("$.results[1].active", is(true)));
    }

    @Test
    void freeTextSearchTaxIdWithMatchedFields() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when showSingleTermMatchedFields=true returns matched fields
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath()
                                                + "?query=10&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].taxonId", is(10)))
                .andExpect(jsonPath("$.matchedFields", notNullValue()))
                .andExpect(jsonPath("$.matchedFields.size()", is(1)))
                .andExpect(jsonPath("$.matchedFields[0].name", is("tax_id_str")))
                .andExpect(jsonPath("$.matchedFields[0].hits", is(1)));
    }

    @Test
    void freeTextSearchMnemonicWithMatchedFields() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when showSingleTermMatchedFields=true returns matched fields mnemonic
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath()
                                                + "?query=mnemonic10&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].taxonId", is(10)))
                .andExpect(jsonPath("$.matchedFields", notNullValue()))
                .andExpect(jsonPath("$.matchedFields.size()", is(1)))
                .andExpect(jsonPath("$.matchedFields[0].name", is("mnemonic")))
                .andExpect(jsonPath("$.matchedFields[0].hits", is(1)));
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
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 20, 15)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific10", "scientific20")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common20")))
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
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(20, 10, 15)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific20", "scientific10")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common20", "common10")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("common_name,mnemonic"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 20, 15)))
                    .resultMatcher(jsonPath("$.results.*.scientificName").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common20")))
                    .resultMatcher(
                            jsonPath("$.results.*.mnemonic", contains("mnemonic10", "mnemonic20")))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("superkingdom,taxonomies_with"))
                    .resultMatcher(jsonPath("$.results.*.taxonId", contains(10, 20, 15)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.scientificName",
                                    contains("scientific10", "scientific20")))
                    .resultMatcher(
                            jsonPath("$.results.*.commonName", contains("common10", "common20")))
                    .resultMatcher(
                            jsonPath("$.results.*.mnemonic", contains("mnemonic10", "mnemonic20")))
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
                                            jsonPath("$.results.*.taxonId", contains(10, 20, 15)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.scientificName",
                                                    contains("scientific10", "scientific20")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.commonName",
                                                    contains("common10", "common20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("10")))
                                    .resultMatcher(content().string(containsString("15")))
                                    .resultMatcher(content().string(containsString("20")))
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
                                    .resultMatcher(content().string(containsString("15")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "20\tmnemonic20\tscientific20\tcommon20\tother names20\t\tfamily\t\t19")))
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
