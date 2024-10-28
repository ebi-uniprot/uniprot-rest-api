package org.uniprot.api.support.data.subcellular.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(SubcellularLocationController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            SubcellularLocationSearchControllerIT.SubcellularLocationSearchContentTypeParamResolver
                    .class,
            SubcellularLocationSearchControllerIT.SubcellularLocationSearchParameterResolver.class
        })
public class SubcellularLocationSearchControllerIT extends AbstractSearchControllerIT {

    @Autowired private SubcellularLocationRepository repository;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer solrBatchSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.SUBCELLULAR_LOCATION;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.subcellularlocation;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/locations/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.SUBCELLLOCATION;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "id":
                value = "SL-0001";
                break;
        }
        return value;
    }

    @Test
    void defaultSearchWithLowercaseId() throws Exception {
        // given
        saveEntries(2);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                getSearchRequestPath() + "?query=(sl-0001)")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].id", is("SL-0001")));
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries).forEach(i -> saveEntry("SL-000" + i));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry("SL-0001");
        saveEntry("SL-0002");
    }

    private void saveEntry(String accession) {
        SubcellularLocationDocument document = SubcellularLocationITUtils.createSolrDoc(accession);
        getStoreManager().saveDocs(DataStoreManager.StoreType.SUBCELLULAR_LOCATION, document);
    }

    static class SubcellularLocationSearchParameterResolver
            extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:SL-0001"))
                    .resultMatcher(jsonPath("$.results.*.name", contains("Name value SL-0001")))
                    .resultMatcher(jsonPath("$.results.*.id", contains("SL-0001")))
                    .resultMatcher(jsonPath("$.results.*.category", contains("Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition", contains("Definition value SL-0001")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("id:SL-9999"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("definition:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.name",
                                    contains("Name value SL-0001", "Name value SL-0002")))
                    .resultMatcher(jsonPath("$.results.*.id", contains("SL-0001", "SL-0002")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    contains(
                                            "Definition value SL-0001",
                                            "Definition value SL-0002")))
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
                    .queryParam("query", Collections.singletonList("id:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("name desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.name",
                                    contains("Name value SL-0002", "Name value SL-0001")))
                    .resultMatcher(jsonPath("$.results.*.id", contains("SL-0002", "SL-0001")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.definition",
                                    contains(
                                            "Definition value SL-0002",
                                            "Definition value SL-0001")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("id,name,category"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.name",
                                    contains("Name value SL-0001", "Name value SL-0002")))
                    .resultMatcher(jsonPath("$.results.*.id").exists()) // required
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.category",
                                    contains("Cellular component", "Cellular component")))
                    .resultMatcher(jsonPath("$.results.*.definition").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder().build();
        }
    }

    static class SubcellularLocationSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:SL-0001 OR id:SL-0002")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    containsInAnyOrder("SL-0001", "SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.name",
                                                    containsInAnyOrder(
                                                            "Name value SL-0001",
                                                            "Name value SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.definition",
                                                    containsInAnyOrder(
                                                            "Definition value SL-0001",
                                                            "Definition value SL-0002")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.category",
                                                    containsInAnyOrder(
                                                            "Cellular component",
                                                            "Cellular component")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("SL-0001")))
                                    .resultMatcher(content().string(containsString("SL-0002")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Subcellular location ID\tDescription\tCategory\tName")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "SL-0001\tDefinition value SL-0001\tCellular component\tName value SL-0001")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "SL-0002\tDefinition value SL-0002\tCellular component\tName value SL-0002")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "format-version: 1.2\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "default-namespace: uniprot:locations\n")))
                                    .resultMatcher(content().string(containsString("[Term]\n")))
                                    .resultMatcher(
                                            content().string(containsString("id: SL-0001\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "name: Name value SL-0001\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "def: \"Definition value SL-0001\" []\n")))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("id:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe subcellular location id filter value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                                    .build())
                    .build();
        }
    }
}
