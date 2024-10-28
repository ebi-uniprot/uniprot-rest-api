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
import org.uniprot.api.aa.repository.UniRuleFacetConfig;
import org.uniprot.api.aa.repository.UniRuleQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.unirule.Information;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.InformationBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmaad
 * @since 2020-11-19
 */
@ContextConfiguration(classes = {AARestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRuleController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniRuleSearchControllerIT.UniRuleSearchContentTypeParamResolver.class,
            UniRuleSearchControllerIT.UniRuleSearchParameterResolver.class
        })
public class UniRuleSearchControllerIT extends AbstractRuleSearchWithFacetControllerIT {
    private static final String OLD_RULE_ID = "PIRSR628829-1";

    @Autowired private UniRuleFacetConfig facetConfig;

    @Autowired private UniRuleQueryRepository repository;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIRULE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.unirule;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/unirule/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIRULE;
    }

    @BeforeAll
    void initDataStore() {
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIRULE,
                        new UniRuleDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "unirule_id":
                value = "UR000000200";
                break;
            case "all_rule_id":
                value = OLD_RULE_ID;
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
        saveEntry(200);
        saveEntry(300);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int suffix) {
        UniRuleEntry entry = UniRuleEntryBuilderTest.createObject(2);
        UniRuleEntry updatedCommentEntry = UniRuleControllerITUtils.updateAllCommentTypes(entry);
        UniRuleEntry uniRuleEntry =
                UniRuleControllerITUtils.updateValidValues(
                        updatedCommentEntry, suffix, UniRuleControllerITUtils.RuleType.UR);
        Information info =
                InformationBuilder.from(entry.getInformation()).oldRuleNum(OLD_RULE_ID).build();
        uniRuleEntry = UniRuleEntryBuilder.from(uniRuleEntry).information(info).build();
        UniRuleDocumentConverter docConverter =
                new UniRuleDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
        UniRuleDocument document = docConverter.convertToDocument(uniRuleEntry);
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIRULE, document);
    }

    static class UniRuleSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("unirule_id:UR000000200"))
                    .resultMatcher(jsonPath("$.results.*.uniRuleId", contains("UR000000200")))
                    .resultMatcher(jsonPath("$.results.*.information").exists())
                    .resultMatcher(jsonPath("$.results.*.mainRule", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.otherRules", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.samFeatureSets", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.positionFeatureSets", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.proteinsAnnotatedCount", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.createdDate", notNullValue()))
                    .resultMatcher(jsonPath("$.results.*.modifiedDate", notNullValue()))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("unirule_id:UR999999999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("unirule_id:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniRuleId",
                                    contains("UR000000200", "UR000000300")))
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
                    .queryParam("query", Collections.singletonList("unirule_id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The unirule_id value has invalid format. It should match the regular expression 'UR[0-9]{9}'")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("unirule_id desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniRuleId",
                                    contains("UR000000300", "UR000000200")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam(
                            "fields",
                            Collections.singletonList(
                                    "rule_id,template_entries,annotation_covered"))
                    .resultMatcher(jsonPath("$.results[*].uniRuleId", is(notNullValue())))
                    .resultMatcher(
                            jsonPath(
                                    "$.results[*].information.uniProtAccessions",
                                    is(notNullValue())))
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
                                    contains("UR000000200", "UR000000300")))
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

    static class UniRuleSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("unirule_id:UR000000200 OR unirule_id:UR000000300")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.uniRuleId",
                                                    containsInAnyOrder(
                                                            "UR000000200", "UR000000300")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("UR000000200")))
                                    .resultMatcher(content().string(containsString("UR000000300")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniRule ID\tTemplate Entries")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UR000000200\taccession-")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UR000000300\taccession-")))
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
                    .query("unirule_id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(is(emptyString()))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The unirule_id value has invalid format. It should match the regular expression 'UR[0-9]{9}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    is(
                                                                            "Error messages\nThe unirule_id value has invalid format. It should match the regular expression 'UR[0-9]{9}'"))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    is(
                                                                            "Error messages\nThe unirule_id value has invalid format. It should match the regular expression 'UR[0-9]{9}'"))))
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
