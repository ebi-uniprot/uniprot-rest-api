package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniparc.controller.UniParcITUtils.getUniParcDocument;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.convertToUniParcEntryLight;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.createUniParcEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
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
import org.uniprot.api.rest.controller.AbstractSearchWithSuggestionsControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.UniParcStreamConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortInMemoryUniParcCrossReferenceStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcLightSearchControllerIT.UniParcSearchContentTypeParamResolver.class,
            UniParcLightSearchControllerIT.UniParcSearchParameterResolver.class
        })
class UniParcLightSearchControllerIT extends AbstractSearchWithSuggestionsControllerIT {
    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private UniParcQueryRepository repository;
    @Autowired private UniParcFacetConfig facetConfig;

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer defaultPageSize;

    private UniParcLightStoreClient storeClient;

    private UniParcCrossReferenceStoreClient xRefStoreClient;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPARC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniparc;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/uniparc/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return defaultPageSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "upi":
            case "uniparc":
                value = UPI_PREF + 11;
                break;
            case "taxonomy_id":
                value = "9606";
                break;
            case "length":
                value = "[* TO *]";
                break;
            case "uniprotkb":
            case "isoform":
            case "accession":
                value = "P10011";
                break;
            case "proteome":
            case "upid":
                value = "UP000005640";
                break;
            case "proteomecomponent":
                value = "\"UP000005640:chromosome\"";
                break;
        }
        return value;
    }

    @BeforeAll
    void initDataStore() {
        storeClient =
                new UniParcLightStoreClient(
                        VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
        getStoreManager().addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, storeClient);

        xRefStoreClient =
                new UniParcCrossReferenceStoreClient(
                        VoldemortInMemoryUniParcCrossReferenceStore.getInstance(
                                "uniparc-cross-reference"));
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, xRefStoreClient);
    }

    @Test
    void searchCanSearchDatabaseFacetsFields() throws Exception {
        // given
        saveEntry(SaveScenario.FACETS_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "*:*")
                                        .param("size", "0")
                                        .param("facets", "database_facet")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].name", is("database_facet")))
                .andExpect(jsonPath("$.facets[0].values.size()", greaterThan(20)))
                .andExpect(jsonPath("$.facets[0].values.*.label", is(notNullValue())))
                .andExpect(jsonPath("$.facets[0].values.*.label", hasItem("UniProtKB")))
                .andExpect(jsonPath("$.facets[0].values.*.label", hasItem("EnsemblBacteria")))
                .andExpect(jsonPath("$.facets[0].values.*.label", hasItem("EMBL CDS")))
                .andExpect(jsonPath("$.facets[0].values.*.count", hasItem(greaterThan(0))));
    }

    @Test
    void testGetNoSuggestionForSearch() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_NOT_FOUND);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=cambridge")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", is(empty())))
                .andExpect(jsonPath("$.suggestions").doesNotExist());
    }

    @Test
    void searchDefaultSearchWithUnderscoreIds() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "WP_168893211")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.uniParcId", contains("UPI0000083A11")));
    }

    @Test
    void searchWithAlias_whenNotExist() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "uniparc:UPI0000083A22")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void searchWithAlias_whenWrongAliasName() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "wrongAlias:UPI0000083A22")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    @Test
    void searchWithChecksumButMD5Value() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);
        String md5 = "0C3E6155A2C45AB6E7825BA88DAEC00B";

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "checksum:" + md5)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.sequence.md5", contains(md5)));
    }

    @Test
    void searchDefaultSearchWithLowercaseId() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "upi0000083a11")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.uniParcId", contains("UPI0000083A11")));
    }

    @Override
    protected List<Triple<String, String, List<String>>> getTriplets() {
        return List.of(
                Triple.of("protein", "proteenname11", List.of("proteinname11")),
                Triple.of("taxonomy_name phrase", "\"homo sapeans\"", List.of("\"homo sapiens\"")),
                Triple.of("taxonomy_name", "homo sapeans", List.of("homo sapiens")),
                Triple.of("gene", "genename21", List.of("genename11", "genename20")));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(11);
        saveEntry(20);
        if (SaveScenario.FACETS_SUCCESS.equals(saveContext)) {
            UniParcEntry entry = createUniParcEntry(30, UPI_PREF);
            UniParcEntryBuilder builder = UniParcEntryBuilder.from(entry);
            Arrays.stream(UniParcDatabase.values())
                    .forEach(
                            db ->
                                    builder.uniParcCrossReferencesAdd(
                                            UniParcCrossReferenceMocker.createUniParcCrossReference(
                                                    db)));
            saveEntry(builder.build());
        }
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        UniParcEntry entry = createUniParcEntry(i, UPI_PREF);
        saveEntry(entry);
    }

    private void saveEntry(UniParcEntry entry) {
        UniParcDocument.UniParcDocumentBuilder builder = getUniParcDocument(entry);
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPARC, builder.build());

        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, entryLight);
        List<UniParcCrossReferencePair> xrefPairs =
                UniParcCrossReferenceMocker.createCrossReferencePairsFromXRefs(
                        entryLight.getUniParcId(),
                        xrefGroupSize,
                        entry.getUniParcCrossReferences());
        for (UniParcCrossReferencePair xrefPair : xrefPairs) {
            xRefStoreClient.saveEntry(xrefPair);
        }
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType())
                .getReturnFields()
                .stream()
                .map(
                        returnField -> {
                            String lightPath =
                                    returnField.getPaths().get(returnField.getPaths().size() - 1);
                            return Arguments.of(
                                    returnField.getName(), Collections.singletonList(lightPath));
                        });
    }

    static class UniParcSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:UPI0000083A11"))
                    .resultMatcher(jsonPath("$.results.*.uniParcId", contains("UPI0000083A11")))
                    .resultMatcher(
                            jsonPath("$.results.*.oldestCrossRefCreated", iterableWithSize(1)))
                    .resultMatcher(
                            jsonPath("$.results.*.mostRecentCrossRefUpdated", iterableWithSize(1)))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:UPI0000083B11"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20")))
                    .resultMatcher(
                            jsonPath("$.results.*.oldestCrossRefCreated", iterableWithSize(2)))
                    .resultMatcher(
                            jsonPath("$.results.*.mostRecentCrossRefUpdated", iterableWithSize(2)))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("taxonomy_name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'taxonomy_name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            List.of(
                                    "upi:INVALID OR taxonomy_id:INVALID "
                                            + "OR length:INVALID OR proteome:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI",
                                            "'length' filter type 'general' is invalid. Expected 'range' filter type",
                                            "The taxonomy id filter value should be a number",
                                            "The 'proteome' value has invalid format. It should be a valid Proteome UPID")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("upi desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A20", "UPI0000083A11")))
                    .resultMatcher(
                            jsonPath("$.results.*.oldestCrossRefCreated", iterableWithSize(2)))
                    .resultMatcher(
                            jsonPath("$.results.*.mostRecentCrossRefUpdated", iterableWithSize(2)))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("upi,gene"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20")))
                    .resultMatcher(jsonPath("$.results[*].geneNames").hasJsonPath())
                    .resultMatcher(jsonPath("$.results[*].uniProtKBAccessions").doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].proteinNames").doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].sequence").doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].sequenceFeatures").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.results.*.oldestCrossRefCreated", iterableWithSize(2)))
                    .resultMatcher(
                            jsonPath("$.results.*.mostRecentCrossRefUpdated", iterableWithSize(2)))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("organism_name,database_facet"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results[*].uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20", "UPI0000083A30")))
                    .resultMatcher(
                            jsonPath("$.results.*.oldestCrossRefCreated", iterableWithSize(3)))
                    .resultMatcher(
                            jsonPath("$.results.*.mostRecentCrossRefUpdated", iterableWithSize(3)))
                    .resultMatcher(jsonPath("$.facets.*.label", contains("Organisms", "Database")))
                    .resultMatcher(jsonPath("$.facets[1].values.size()", greaterThan(20)))
                    .resultMatcher(jsonPath("$.facets[1].values.*.value", hasItem("100")))
                    .resultMatcher(jsonPath("$.facets[1].values.*.count", hasItem(3)))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values.*.value",
                                    contains("Homo sapiens", "Torpedo californica")))
                    .resultMatcher(jsonPath("$.facets[0].values.*.count", contains(3, 3)))
                    .build();
        }
    }

    static class UniParcSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("taxonomy_id:9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.uniParcId",
                                                    contains("UPI0000083A11", "UPI0000083A20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A11")))
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UPI0000083A11	Name 7787; Name 9606	P10011; P12311	2017-02-12	2017-04-23	21")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UPI0000083A20	Name 7787; Name 9606	P10020; P12320	2017-02-12	2017-04-23	30")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">UPI0000083A11 status=active")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().contentType(MediaType.APPLICATION_XML))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>UPI0000083A11</accession>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<sequence length=\"30\" checksum=\"F1F61B3F5B5121E3\">MLMPKRTKYRAAAAAAAAAAAAAAAAAAAA</sequence>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>UPI0000083A20</accession>\n"
                                                                            + "  <signatureSequenceMatch database=\"CDD\" id=\"SIG000020\">\n"
                                                                            + "    <ipr name=\"Inter Pro Name20\" id=\"IP000020\"/>\n"
                                                                            + "    <lcn start=\"12\" end=\"23\" alignment=\"55M\"/>\n"
                                                                            + "    <lcn start=\"45\" end=\"89\"/>\n"
                                                                            + "  </signatureSequenceMatch>")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("proteome:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'proteome' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'proteome' value has invalid format. It should be a valid Proteome UPID"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'proteome' value has invalid format. It should be a valid Proteome UPID"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'proteome' value has invalid format. It should be a valid Proteome UPID"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'proteome' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .build();
        }
    }
}
