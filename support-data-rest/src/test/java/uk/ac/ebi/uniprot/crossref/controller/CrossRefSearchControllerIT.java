package uk.ac.ebi.uniprot.crossref.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.crossref.config.CrossRefFacetConfig;
import uk.ac.ebi.uniprot.api.crossref.controller.CrossRefController;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.api.taxonomy.TaxonomyController;
import uk.ac.ebi.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntryBuilder;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.domain.taxonomy.builder.TaxonomyEntryBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;
import uk.ac.ebi.uniprot.search.field.CrossRefField;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(CrossRefController.class)
@ExtendWith(value = {SpringExtension.class, CrossRefSearchControllerIT.CrossRefSearchContentTypeParamResolver.class,
        CrossRefSearchControllerIT.CrossRefSearchParameterResolver.class})
public class CrossRefSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static String SEARCH_ACCESSION1 = "DB-" + ThreadLocalRandom.current().nextLong(1000, 9999);
    private static String SEARCH_ACCESSION2 = "DB-" + ThreadLocalRandom.current().nextLong(1000, 9999);
    private static List<String> SORTED_ACCESSIONS = new ArrayList<>(Arrays.asList(SEARCH_ACCESSION1, SEARCH_ACCESSION2));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Autowired
    private CrossRefFacetConfig facetConfig;

    @Override
    protected void cleanEntries() {
        this.storeManager.cleanSolr(DataStoreManager.StoreType.CROSSREF);
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
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
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry(i));
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
        CrossRefEntry crossRefEntry = entryBuilder.accession(accession)
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

        CrossRefDocument document = CrossRefDocument.builder()
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

        this.storeManager.saveDocs(DataStoreManager.StoreType.CROSSREF, document);
    }


    static class CrossRefSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:" + SEARCH_ACCESSION1))
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
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.size()", is(2)))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("'name' filter type 'range' is invalid. Expected 'term' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:[INVALID to INVALID123] OR name:123"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", containsInAnyOrder(
                            "query parameter has an invalid syntax")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            Collections.sort(SORTED_ACCESSIONS);
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("accession asc"))
                    .resultMatcher(jsonPath("$.results.*.accession", contains(SORTED_ACCESSIONS.get(0), SORTED_ACCESSIONS.get(1))))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("accession,name"))
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.reviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.id", notNullValue()))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("category_facet"))
                    .resultMatcher(jsonPath("$.results.*.category", containsInAnyOrder("Family and domain databases20", "Family and domain databases10")))
                    .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name", contains("category_facet")))
                    .build();
        }
    }


    static class CrossRefSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("accession:" + SEARCH_ACCESSION1 + " OR accession:" + SEARCH_ACCESSION2)
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.results.*.accession", containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                            .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("random_field:invalid")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*", contains("'random_field' is not a valid search field")))
                            .build())
                    .build();
        }
    }

}
