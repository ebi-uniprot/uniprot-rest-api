package org.uniprot.api.crossref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.api.crossref.repository.CrossRefRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.crossref.CrossRefEntry;
import org.uniprot.core.crossref.CrossRefEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;
import org.uniprot.store.search.field.CrossRefField;
import org.uniprot.store.search.field.SearchField;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(CrossRefController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            CrossRefSearchControllerIT.CrossRefSearchContentTypeParamResolver.class,
            CrossRefSearchControllerIT.CrossRefSearchParameterResolver.class
        })
public class CrossRefSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static final String SEARCH_ACCESSION1 =
            "DB-" + ThreadLocalRandom.current().nextLong(1000, 9999);
    private static final String SEARCH_ACCESSION2 =
            "DB-" + ThreadLocalRandom.current().nextLong(1000, 9999);
    private static final List<String> SORTED_ACCESSIONS =
            new ArrayList<>(Arrays.asList(SEARCH_ACCESSION1, SEARCH_ACCESSION2));

    @Autowired private CrossRefRepository repository;

    @Autowired private CrossRefFacetConfig facetConfig;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.CROSSREF;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.crossref;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/xref/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected List<SearchField> getAllSearchFields() {
        return Arrays.asList(CrossRefField.Search.values());
    }

    @Override
    protected String getFieldValueForValidatedField(SearchField searchField) {
        String value = "";
        if ("accession".equalsIgnoreCase(searchField.getName())) {
            return SEARCH_ACCESSION1;
        }
        return value;
    }

    @Override
    protected List<String> getAllSortFields() {
        return Arrays.stream(CrossRefField.Sort.values())
                .map(CrossRefField.Sort::name)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(CrossRefField.ResultFields.values())
                .map(CrossRefField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(SEARCH_ACCESSION1, 10);
        saveEntry(SEARCH_ACCESSION2, 20);
    }

    private void saveEntry(long suffix) {
        String accPrefix = "DI-";
        long num = ThreadLocalRandom.current().nextLong(1000, 9999);
        String accession = accPrefix + num;
        saveEntry(accession, suffix);
    }

    private void saveEntry(String accession, long suffix) {
        CrossRefEntryBuilder entryBuilder = new CrossRefEntryBuilder();
        CrossRefEntry crossRefEntry =
                entryBuilder
                        .accession(accession)
                        .abbrev("TIGRFAMs" + suffix)
                        .name("TIGRFAMs; a protein family database" + suffix)
                        .pubMedId("17151080" + suffix)
                        .doiId("10.1093/nar/gkl1043" + suffix)
                        .linkType("Explicit" + suffix)
                        .server("http://tigrfams.jcvi.org/cgi-bin/index.cgi" + suffix)
                        .dbUrl("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s" + suffix)
                        .category("Family and domain databases" + suffix)
                        .reviewedProteinCount(10L + suffix)
                        .unreviewedProteinCount(5L + suffix)
                        .build();

        CrossRefDocument document =
                CrossRefDocument.builder()
                        .accession(crossRefEntry.getAccession())
                        .abbrev(crossRefEntry.getAbbrev())
                        .name(crossRefEntry.getName())
                        .pubMedId(crossRefEntry.getPubMedId())
                        .doiId(crossRefEntry.getDoiId())
                        .linkType(crossRefEntry.getLinkType())
                        .server(crossRefEntry.getServer())
                        .dbUrl(crossRefEntry.getDbUrl())
                        .category(crossRefEntry.getCategory())
                        .reviewedProteinCount(crossRefEntry.getReviewedProteinCount())
                        .unreviewedProteinCount(crossRefEntry.getUnreviewedProteinCount())
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.CROSSREF, document);
    }

    static class CrossRefSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query", Collections.singletonList("accession:" + SEARCH_ACCESSION1))
                    .resultMatcher(jsonPath("$.results.*.accession", contains(SEARCH_ACCESSION1)))
                    .resultMatcher(jsonPath("$.results.length()", is(1)))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:DB-0000"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.accession",
                                    containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.size()", is(2)))
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
                                            "'name' filter type 'range' is invalid. Expected 'term' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "accession:[INVALID to INVALID123] OR name:123"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder("query parameter has an invalid syntax")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            Collections.sort(SORTED_ACCESSIONS);
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("accession asc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.accession",
                                    contains(SORTED_ACCESSIONS.get(0), SORTED_ACCESSIONS.get(1))))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("accession,name"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.accession",
                                    containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.reviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.id", notNullValue()))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("category_facet"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    containsInAnyOrder(
                                            "Family and domain databases20",
                                            "Family and domain databases10")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.accession",
                                    containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name", contains("category_facet")))
                    .build();
        }
    }

    static class CrossRefSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("accession:" + SEARCH_ACCESSION1 + " OR accession:" + SEARCH_ACCESSION2)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.accession",
                                                    containsInAnyOrder(
                                                            SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("random_field:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "'random_field' is not a valid search field")))
                                    .build())
                    .build();
        }
    }
}
