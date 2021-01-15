package org.uniprot.api.support.data.disease.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.support.data.disease.controller.download.IT.BaseDiseaseDownloadIT.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            DiseaseSearchContentTypeParamResolver.class,
            DiseaseSearchControllerIT.DiseaseSearchParameterResolver.class
        })
public class DiseaseSearchControllerIT extends AbstractSearchControllerIT {

    @Autowired private DiseaseRepository repository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.DISEASE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.disease;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/disease/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.DISEASE;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        if ("id".equalsIgnoreCase(searchField)) {
            return SEARCH_ACCESSION1;
        }
        return value;
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
        long num = ThreadLocalRandom.current().nextLong(10000, 99999);
        String accession = accPrefix + num;
        saveEntry(accession, suffix);
    }

    private void saveEntry(String accession, long suffix) {
        DiseaseEntryBuilder diseaseBuilder = new DiseaseEntryBuilder();
        KeywordId keyword =
                new KeywordIdBuilder()
                        .name("Mental retardation" + suffix)
                        .id("KW-0991" + suffix)
                        .build();
        DiseaseCrossReference xref1 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MIM" + suffix)
                        .id("617140" + suffix)
                        .propertiesAdd("phenotype" + suffix)
                        .build();
        DiseaseCrossReference xref2 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MedGen" + suffix)
                        .id("CN238690" + suffix)
                        .build();
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MeSH" + suffix)
                        .id("D000015" + suffix)
                        .build();
        DiseaseCrossReference xref4 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MeSH" + suffix)
                        .id("D008607" + suffix)
                        .build();
        DiseaseEntry diseaseEntry =
                diseaseBuilder
                        .name("ZTTK syndrome" + suffix)
                        .id(accession)
                        .acronym("ZTTKS" + suffix)
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .keywordsAdd(keyword)
                        .reviewedProteinCount(suffix)
                        .unreviewedProteinCount(suffix)
                        .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds =
                    diseaseEntry.getKeywords().stream()
                            .map(KeywordId::getName)
                            .collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name =
                Stream.concat(
                                Stream.concat(
                                        Stream.of(
                                                diseaseEntry.getName(),
                                                diseaseEntry.getAcronym(),
                                                diseaseEntry.getDefinition()),
                                        kwIds.stream()),
                                diseaseEntry.getAlternativeNames().stream())
                        .collect(Collectors.toList());
        DiseaseDocument document =
                DiseaseDocument.builder()
                        .id(accession)
                        .name(name)
                        .diseaseObj(getDiseaseBinary(diseaseEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.DISEASE, document);
    }

    private ByteBuffer getDiseaseBinary(DiseaseEntry entry) {
        try {
            return ByteBuffer.wrap(
                    DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse DiseaseEntry entry to binary json: ", e);
        }
    }

    static class DiseaseSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:" + SEARCH_ACCESSION1))
                    .resultMatcher(jsonPath("$.results.*.id", contains(SEARCH_ACCESSION1)))
                    .resultMatcher(jsonPath("$.results.length()", is(1)))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:DI-00000"))
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
                                            "'name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList("id:[INVALID to INVALID123] OR name:123"))
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
                    .queryParam("sort", Collections.singletonList("id asc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains(SORTED_ACCESSIONS.get(0), SORTED_ACCESSIONS.get(1))))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,name,definition"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.reviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.id").exists())
                    .resultMatcher(jsonPath("$.results.*.name").exists())
                    .resultMatcher(jsonPath("$.results.*.definition").exists())
                    .build();
        }

        @Override // duplicate test to satisfy the parent test
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reviewed"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    containsInAnyOrder(SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                    .resultMatcher(jsonPath("$.results.*.name", notNullValue()))
                    .build();
        }
    }
}
