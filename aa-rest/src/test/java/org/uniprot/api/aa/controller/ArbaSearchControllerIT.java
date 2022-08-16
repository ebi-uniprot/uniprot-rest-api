package org.uniprot.api.aa.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.aa.AARestApplication;
import org.uniprot.api.aa.repository.ArbaFacetConfig;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.InformationBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.arba.ArbaDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, AARestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ArbaController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ArbaSearchControllerIT.ArbaSearchContentTypeParamResolver.class,
            ArbaSearchControllerIT.ArbaSearchParameterResolver.class
        })
public class ArbaSearchControllerIT extends AbstractRuleSearchWithFacetControllerIT {

    @Autowired private ArbaFacetConfig facetConfig;

    @Autowired private ArbaQueryRepository repository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.ARBA;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.arba;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/arba/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.ARBA;
    }

    @BeforeAll
    void initDataStore() {
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.ARBA,
                        new UniRuleDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        if ("rule_id".equals(searchField)) {
            value = "ARBA00000200";
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(200);
        saveEntry(300);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int suffix) {
        UniRuleEntry entry = UniRuleEntryBuilderTest.createObject(2);
        // remove unnecessary fields for ARBA
        var builder = UniRuleEntryBuilder.from(entry);
        InformationBuilder infoBuilder = new InformationBuilder("0");
        builder.information(infoBuilder.build());
        UniRuleEntry thinUniRuleEntry =
                builder.uniRuleId(entry.getUniRuleId())
                        .otherRulesSet(null)
                        .positionFeatureSetsSet(null)
                        .samFeatureSetsSet(null)
                        .build();
        UniRuleEntry updatedCommentEntry =
                UniRuleControllerITUtils.updateAllCommentTypes(thinUniRuleEntry);
        UniRuleEntry arbaEntry =
                UniRuleControllerITUtils.updateValidValues(
                        updatedCommentEntry, suffix, UniRuleControllerITUtils.RuleType.ARBA);
        var docConverter = new ArbaDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
        var arbaDocument = docConverter.convertToDocument(arbaEntry);
        getStoreManager().saveDocs(getStoreType(), arbaDocument);
    }

    static class ArbaSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("rule_id:ARBA00000200"))
                    .resultMatcher(jsonPath("$.results.*.uniRuleId", contains("ARBA00000200")))
                    .resultMatcher(jsonPath("$.results.*.information").exists())
                    .resultMatcher(jsonPath("$.results.*.mainRule", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.otherRules").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.samFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.positionFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.proteinsAnnotatedCount", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.createdDate", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.modifiedDate", notNullValue()))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("rule_id:ARBA99999999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("rule_id:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniRuleId",
                                    contains("ARBA00000200", "ARBA00000300")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("protein_name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'protein_name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("rule_id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The rule_id value has invalid format. It should match the regular expression 'ARBA[0-9]{8}'")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("rule_id desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniRuleId",
                                    contains("ARBA00000300", "ARBA00000200")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("rule_id,annotation_covered"))
                    .resultMatcher(jsonPath("$.results[*].uniRuleId", is(notNullValue())))
                    .resultMatcher(
                            jsonPath("$.results[*].information.uniProtAccessions").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.results[*].mainRule.annotations", is(notNullValue())))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("superkingdom"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniRuleId",
                                    contains("ARBA00000200", "ARBA00000300")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets[0].label", is("Superkingdom")))
                    .resultMatcher(jsonPath("$.facets[0].name", is("superkingdom")))
                    .resultMatcher(jsonPath("$.facets[0].allowMultipleSelection", is(true)))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values.*.label",
                                    containsInAnyOrder(
                                            "Archaea", "Bacteria", "Eukaryota", "Viruses")))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values.*.value",
                                    containsInAnyOrder(
                                            "Archaea", "Bacteria", "Eukaryota", "Viruses")))
                    .resultMatcher(
                            jsonPath("$.facets[0].values.*.count", containsInAnyOrder(2, 2, 2, 2)))
                    .build();
        }
    }

    static class ArbaSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("rule_id:ARBA00000200 OR rule_id:ARBA00000300")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.uniRuleId",
                                                    containsInAnyOrder(
                                                            "ARBA00000200", "ARBA00000300")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("ARBA00000200")))
                                    .resultMatcher(content().string(containsString("ARBA00000300")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("rule_id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(is(emptyString()))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The rule_id value has invalid format. It should match the regular expression 'ARBA[0-9]{8}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    is(
                                                                            "Error messages\nThe rule_id value has invalid format. It should match the regular expression 'ARBA[0-9]{8}'"))))
                                    .build())
                    .build();
        }
    }
}
