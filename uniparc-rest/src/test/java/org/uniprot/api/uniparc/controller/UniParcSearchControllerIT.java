package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.*;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.api.uniparc.repository.store.UniParcStreamConfig;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

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
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcSearchControllerIT.UniParcSearchContentTypeParamResolver.class,
            UniParcSearchControllerIT.UniParcSearchParameterResolver.class
        })
class UniParcSearchControllerIT extends AbstractSearchWithFacetControllerIT {
    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private UniParcQueryRepository repository;
    @Autowired private UniParcFacetConfig facetConfig;

    private UniParcStoreClient storeClient;

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
        return 25;
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
                value = "P10011";
                break;
            case "upid":
                value = "UP000005640";
                break;
        }
        return value;
    }

    @BeforeAll
    void initDataStore() {
        storeClient =
                new UniParcStoreClient(
                        VoldemortInMemoryUniParcEntryStore.getInstance("avro-uniparc"));
        getStoreManager().addStore(DataStoreManager.StoreType.UNIPARC, storeClient);

        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIPARC,
                        new UniParcDocumentConverter(
                                TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));
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

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(11);
        saveEntry(20);
        if (SaveScenario.FACETS_SUCCESS.equals(saveContext)) {
            UniParcEntry entry = createEntry(30, UPI_PREF);
            UniParcEntryBuilder builder = UniParcEntryBuilder.from(entry);
            Arrays.stream(UniParcDatabase.values())
                    .forEach(db -> builder.uniParcCrossReferencesAdd(getXref(db)));
            saveEntry(builder.build());
        }
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        UniParcEntry entry = createEntry(i, UPI_PREF);
        saveEntry(entry);
    }

    private void saveEntry(UniParcEntry entry) {
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);

        getStoreManager().saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC, entry);
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
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
                            Collections.singletonList(
                                    "upi:INVALID OR taxonomy_id:INVALID "
                                            + "OR length:INVALID OR upid:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI",
                                            "'length' filter type 'general' is invalid. Expected 'range' filter type",
                                            "The taxonomy id filter value should be a number",
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
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
                    .resultMatcher(
                            jsonPath("$.results[*].uniParcCrossReferences[*].geneName")
                                    .hasJsonPath())
                    .resultMatcher(
                            jsonPath("$.results[*].uniParcCrossReferences[*].taxonomy")
                                    .doesNotExist())
                    .resultMatcher(
                            jsonPath("$.results[*].uniParcCrossReferences[*].proteinName")
                                    .doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].sequence").doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].sequenceFeatures").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.oldestCrossRefCreated").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.mostRecentCrossRefUpdated").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("database_facet,organism_name"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results[*].uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20", "UPI0000083A30")))
                    .resultMatcher(jsonPath("$.facets.*.label", contains("Organisms", "Database")))
                    .resultMatcher(jsonPath("$.facets[1].values.size()", greaterThan(20)))
                    .resultMatcher(jsonPath("$.facets[1].values.*.value", hasItem("1")))
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
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A11")))
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A20")))
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
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
                                    .build())
                    .build();
        }
    }
}
