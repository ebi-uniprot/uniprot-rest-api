package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.ACC_PREF;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.ID_PREF_50;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.createEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
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
import org.uniprot.api.rest.controller.AbstractSearchWithSuggestionsControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.converters.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;

/**
 * @author jluo
 * @date: 27 Aug 2019
 */
@ContextConfiguration(
        classes = {
            UniRefDataStoreTestConfig.class,
            UniRefRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefEntryLightController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniRefLightSearchControllerIT.UniRefSearchContentTypeParamResolver.class,
            UniRefLightSearchControllerIT.UniRefSearchParameterResolver.class
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefLightSearchControllerIT extends AbstractSearchWithSuggestionsControllerIT {

    @Autowired private UniRefQueryRepository repository;

    @Autowired private UniRefFacetConfig facetConfig;

    private UniRefLightStoreClient storeClient;

    @BeforeAll
    void initDataStore() {
        storeClient =
                new UniRefLightStoreClient(
                        VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref-light"));
        getStoreManager().addStore(DataStoreManager.StoreType.UNIREF_LIGHT, storeClient);
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIREF_LIGHT,
                        new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
    }

    @Test
    void searchInvalidCompleteValueReturnBadRequest() throws Exception {
        // given
        UniRefEntry entry = UniRefEntryMocker.createEntry(1, 12, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=*&complete=invalid")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "'complete' parameter value has invalid format. It should be true or false.")));
    }

    @Test
    void searchByDefaultReturnTopTenMembersAndOrganisms() throws Exception {
        // given
        UniRefEntry entry = UniRefEntryMocker.createEntry(1, 12, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=*")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].id", contains("UniRef50_P03901")))
                .andExpect(
                        jsonPath(
                                "$.results[*].representativeMember.accessions",
                                contains(contains("P12301"))))
                .andExpect(jsonPath("$.results[*].seedId", contains("P12301")))
                .andExpect(jsonPath("$.results[*].members.size()", contains(10)))
                .andExpect(
                        jsonPath(
                                "$.results[*].members[*]",
                                contains(
                                        "P12301", "P32101", "P32102", "P32103", "P32104", "P32105",
                                        "P32106", "P32107", "P32108", "P32109")))
                .andExpect(jsonPath("$.results[*].organisms.size()", contains(10)))
                .andExpect(
                        jsonPath(
                                "$.results[*].organisms[*].scientificName",
                                contains(
                                        "Homo sapiens",
                                        "Homo sapiens 1",
                                        "Homo sapiens 2",
                                        "Homo sapiens 3",
                                        "Homo sapiens 4",
                                        "Homo sapiens 5",
                                        "Homo sapiens 6",
                                        "Homo sapiens 7",
                                        "Homo sapiens 8",
                                        "Homo sapiens 9")))
                .andExpect(
                        jsonPath(
                                "$.results[*].organisms[*].taxonId",
                                contains(
                                        9600, 9607, 9608, 9609, 9610, 9611, 9612, 9613, 9614,
                                        9615)))
                .andExpect(jsonPath("$.results[*].memberCount", contains(12)))
                .andExpect(jsonPath("$.results[*].organismCount", contains(12)));
    }

    @Test
    void searchByDefaultReturnCompleteMembersAndOrganisms() throws Exception {
        // given
        UniRefEntry entry = UniRefEntryMocker.createEntry(1, 12, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=*&complete=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].id", contains("UniRef50_P03901")))
                .andExpect(jsonPath("$.results[*].members.size()", contains(12)))
                .andExpect(jsonPath("$.results[*].organisms.size()", contains(12)))
                .andExpect(jsonPath("$.results[*].memberCount", contains(12)))
                .andExpect(jsonPath("$.results[*].organismCount", contains(12)));
    }

    @Test
    void searchByUniRefId() throws Exception {
        // given
        UniRefEntry entry = UniRefEntryMocker.createEntry(1, 12, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath() + "?query=UniRef50_P03901")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[*].id", contains("UniRef50_P03901")));
    }

    @Test
    void returnFieldsWithAlias_returnsSuccess() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "UniRef50_P03911")
                                        .param("fields", "id,created")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[*].id", contains("UniRef50_P03911")))
                .andExpect(jsonPath("$.results[*].updated", contains("2019-08-27")));
    }

    @Test
    void searchWithAlias_returnsSuccess() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "created:[2019-08-26 TO *]")
                                        .param("sort", "created desc")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath("$.results[*].id", contains("UniRef50_P03911", "UniRef50_P03920")))
                .andExpect(jsonPath("$.results[*].updated", contains("2019-08-27", "2019-08-27")));
    }

    @Override
    protected List<Triple<String, String, List<String>>> getTriplets() {
        return List.of(
                Triple.of("taxonomy_name phrase", "\"homo sapeans\"", List.of("\"homo sapiens\"")),
                Triple.of("taxonomy_name", "homo sapeans", List.of("homo sapiens")),
                Triple.of("name", "MoeK6", List.of("moek5")));
    }

    @AfterEach
    void cleanStoreClient() {
        storeClient.truncate();
    }

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIREF_LIGHT;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniref;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/uniref/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "*";
        switch (searchField) {
            case "id":
                value = ID_PREF_50 + 11;
                break;
            case "taxonomy_id":
                value = "9600";
                break;
            case "length":
                value = "[10 TO 500]";

                break;
            case "count":
                value = "[2 TO 2]";
                break;
            case "uniprot_id":
                value = ACC_PREF + 11;
                break;
            case "date_modified":
            case "created":
                value = "[2000-01-01 TO *]";
                break;
            case "uniparc":
                value = "UPI0000083A11";
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
        saveEntry(11);
        saveEntry(20);
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

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        UniRefEntry unirefEntry = createEntry(i, UniRefType.UniRef50);
        saveEntry(unirefEntry);
    }

    private void saveEntry(UniRefEntry unirefEntry) {
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry entry = converter.toXml(unirefEntry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(entry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIREF_LIGHT, entryLight);
        getStoreManager().saveEntriesInSolr(DataStoreManager.StoreType.UNIREF_LIGHT, entry);
    }

    static class UniRefSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:UniRef50_P03911"))
                    .resultMatcher(jsonPath("$.results.*.id", contains("UniRef50_P03911")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:UniRef50_P03931"))
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
                                    contains("UniRef50_P03911", "UniRef50_P03920")))
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
                                    "id:INVALID OR taxonomy_id:INVALID "
                                            + "OR length:INVALID OR count:INVALID  OR uniparc:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'id' value has invalid format. It should be a valid UniRef Cluster id",
                                            "The taxonomy id filter value should be a number",
                                            "'length' filter type 'general' is invalid. Expected 'range' filter type",
                                            "'count' filter type 'general' is invalid. Expected 'range' filter type",
                                            "The 'uniparc' value has invalid format. It should be a valid UniParc UPI")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("id desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("UniRef50_P03920", "UniRef50_P03911")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,name,date_modified"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results[*].id",
                                    contains("UniRef50_P03911", "UniRef50_P03920")))
                    .resultMatcher(
                            jsonPath("$.results[*].updated", contains("2019-08-27", "2019-08-27")))
                    .resultMatcher(jsonPath("$.results[*].name").hasJsonPath())
                    .resultMatcher(jsonPath("$.results[*].members").doesNotExist())
                    .resultMatcher(jsonPath("$.results[*].representativeMember").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("identity"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.id",
                                    contains("UniRef50_P03911", "UniRef50_P03920")))
                    .build();
        }
    }

    static class UniRefSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("taxonomy_id:9600")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    contains("UniRef50_P03911", "UniRef50_P03920")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString("UniRef50_P03911")))
                                    .resultMatcher(
                                            content().string(containsString("UniRef50_P03920")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Cluster ID\tCluster Name\tCommon taxon\tSize\tDate of last modification")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniRef50_P03911	Cluster: MoeK5 11	Homo sapiens	2	2019-08-27")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniRef50_P03920	Cluster: MoeK5 20	Homo sapiens	2	2019-08-27")))
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
                                                            containsString(
                                                                    ">UniRef50_P03911 some protein name n=2 Tax=Homo sapiens TaxID=9606 RepID=P12311_HUMAN")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">UniRef50_P03920 some protein name n=2 Tax=Homo sapiens TaxID=9606 RepID=P12320_HUMAN")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("uniparc:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'uniparc' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'uniparc' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'uniparc' value has invalid format. It should be a valid UniParc UPI"))
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
                                                            "Error messages\nThe 'uniparc' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .build();
        }
    }
}
