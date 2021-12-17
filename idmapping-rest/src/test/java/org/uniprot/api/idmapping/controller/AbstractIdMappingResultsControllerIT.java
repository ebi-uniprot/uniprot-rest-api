package org.uniprot.api.idmapping.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RECORDS;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingResultsControllerIT extends AbstractIdMappingBasicControllerIT {

    @Autowired protected IdMappingJobCacheService idMappingJobCacheService;

    protected abstract FacetConfig getFacetConfig();

    // ---------------------------------------------------------------------------------
    // -------------------------------- PAGINATION TEST --------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void testIdMappingResultOnePage() throws Exception {
        // when
        Integer defaultPageSize = 5;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = performRequest(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(X_TOTAL_RECORDS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(defaultPageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(ids[0], ids[1], ids[2], ids[3], ids[4])));
    }

    @Test
    void testIdMappingResultWithSize() throws Exception {
        // when
        Integer size = 10;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(X_TOTAL_RECORDS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[0], ids[1], ids[2], ids[3], ids[4], ids[5], ids[6],
                                        ids[7], ids[8], ids[9])));
    }

    @Test
    void testIdMappingResultWithSizeAndPagination() throws Exception {
        // when
        Integer size = 10;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[0], ids[1], ids[2], ids[3], ids[4], ids[5], ids[6],
                                        ids[7], ids[8], ids[9])));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        // when 2nd page
        response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[10], ids[11], ids[12], ids[13], ids[14], ids[15],
                                        ids[16], ids[17], ids[18], ids[19])));
    }

    @Test
    void testIdMappingResultMappingWithZeroSize() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "0"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(X_TOTAL_RECORDS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testIdMappingResultWithNegativeSize() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "-1"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @Test
    void testIdMappingResultWithMoreThan500Size() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "600"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    // ---------------------------------------------------------------------------------
    // -------------------------------- JOB TESTS --------------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void testIdMappingWithUnmappedIds() throws Exception {
        // when
        List<String> unmappedIds = List.of("UnMappedId1", "UnMappedId2");
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        job.getIdMappingResult().setUnmappedIds(unmappedIds);

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(jsonPath("$.failedIds", contains("UnMappedId1", "UnMappedId2")));
    }

    // ---------------------------------------------------------------------------------
    // -------------------------------- SEARCH FIELDS ----------------------------------
    // ---------------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] search fieldName {0}")
    @MethodSource("getAllSearchFields")
    void searchCanSearchWithAllSearchFields(String searchField) throws Exception {
        assertThat(searchField, notNullValue());

        // when
        String fieldValue = getFieldValueForField(searchField);
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .param("query", searchField + ":" + fieldValue)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    // ---------------------------------------------------------------------------------
    // ----------------------------------------- TEST FACETS ---------------------------
    // ---------------------------------------------------------------------------------
    @Test
    void testUniProtKBToUniProtKBMappingWithIncorrectFacetField() throws Exception {

        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("facets", "invalid, invalid2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        startsWith(
                                                "Invalid facet name 'invalid'. Expected value can be "),
                                        startsWith(
                                                "Invalid facet name 'invalid2'. Expected value can be "))));
    }

    @ParameterizedTest(name = "[{index}] facet field name {0}")
    @MethodSource("getAllFacets")
    void testUniProtKBToUniProtKBMappingWithFacet(String facetName) throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxIdsWithFacets);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("facets", facetName)
                                        .param("size", "0"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets.*.name", contains(facetName)))
                .andExpect(jsonPath("$.facets[0].values.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets[0].values.*.count", hasItem(greaterThan(0))));
    }

    @Test
    void testEnrichedResultsWithFacetNotSupportedWarning() throws Exception {
        // when
        Integer defaultPageSize = 5;
        String facetName = getFacetConfig().getFacetNames().stream().findFirst().get();
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxToIdsEnrichAllowed);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("facets", facetName));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(defaultPageSize)))
                .andExpect(jsonPath("$.facets").doesNotExist())
                .andExpect(jsonPath("$.warnings").exists())
                .andExpect(jsonPath("$.warnings.size()", is(1)))
                .andExpect(jsonPath("$.warnings[0].code", is(20)))
                .andExpect(jsonPath("$.warnings[0].message", is("Filters are not supported for mapping results with IDs more than 10")));
    }


    private Stream<Arguments> getAllSearchFields() {
        return SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .map(Arguments::of);
    }

    private Stream<Arguments> getAllFacets() {
        return getFacetConfig().getFacetNames().stream().map(Arguments::of);
    }

    private String getFieldValueForField(String searchField) {
        String value = getFieldValueForValidatedField(searchField);
        if (value.isEmpty()) {
            if (fieldValueIsValid(searchField, "*")) {
                value = "*";
            } else if (fieldValueIsValid(searchField, "true")) {
                value = "true";
            }
        }
        return value;
    }

    private boolean fieldValueIsValid(String field, String value) {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType());
        return fieldConfig.isSearchFieldValueValid(field, value);
    }
}
