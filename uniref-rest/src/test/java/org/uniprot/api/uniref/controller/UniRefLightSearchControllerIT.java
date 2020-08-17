package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.uniprot.api.uniref.controller.UniRefControllerITUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.*;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.search.SolrCollection;

/**
 * @author jluo
 * @date: 27 Aug 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefLightSearchController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniRefLightSearchControllerIT.UniRefSearchContentTypeParamResolver.class,
            UniRefLightSearchControllerIT.UniRefSearchParameterResolver.class
        })
class UniRefLightSearchControllerIT extends AbstractSearchWithFacetControllerIT {

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
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 12, UniRefType.UniRef50);
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
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 12, UniRefType.UniRef50);
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
                                "$.results[*].organisms[*]",
                                contains(
                                        "Homo sapiens (Representative)",
                                        "Homo sapiens 1",
                                        "Homo sapiens 2",
                                        "Homo sapiens 3",
                                        "Homo sapiens 4",
                                        "Homo sapiens 5",
                                        "Homo sapiens 6",
                                        "Homo sapiens 7",
                                        "Homo sapiens 8",
                                        "Homo sapiens 9")))
                .andExpect(jsonPath("$.results[*].organismIds.size()", contains(10)))
                .andExpect(
                        jsonPath(
                                "$.results[*].organismIds[*]",
                                contains(
                                        9600, 9607, 9608, 9609, 9610, 9611, 9612, 9613, 9614,
                                        9615)))
                .andExpect(jsonPath("$.results[*].memberCount", contains(12)))
                .andExpect(jsonPath("$.results[*].organismCount", contains(12)));
    }

    @Test
    void searchByDefaultReturnCompleteMembersAndOrganisms() throws Exception {
        // given
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 12, UniRefType.UniRef50);
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
                .andExpect(jsonPath("$.results[*].organismIds.size()", contains(12)))
                .andExpect(jsonPath("$.results[*].memberCount", contains(12)))
                .andExpect(jsonPath("$.results[*].organismCount", contains(12)));
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
            case "created":
                value = "[* TO *]";
                break;
            case "upi":
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
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType()).getReturnFields()
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
        getStoreManager()
                .saveToStore(
                        DataStoreManager.StoreType.UNIREF_LIGHT, createEntryLight(unirefEntry));
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
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
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
                                            + "OR length:INVALID OR count:INVALID  OR upi:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'id' value has invalid format. It should be a valid UniRef Cluster id",
                                            "The taxonomy id filter value should be a number",
                                            "'length' filter type 'general' is invalid. Expected 'range' filter type",
                                            "'count' filter type 'general' is invalid. Expected 'range' filter type",
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
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
                    .queryParam("fields", Collections.singletonList("id,name"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results[*].id",
                                    contains("UniRef50_P03911", "UniRef50_P03920")))
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
                                                                    "Cluster ID\tCluster Name\tCommon taxon\tSize\tDate of creation")))
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
                                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("upi:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
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
