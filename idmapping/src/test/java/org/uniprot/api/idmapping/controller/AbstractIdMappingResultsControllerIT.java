package org.uniprot.api.idmapping.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingResultsControllerIT extends AbstractStreamControllerIT {

    @Autowired protected IdMappingJobCacheService idMappingJobCacheService;

    protected abstract MockMvc getMockMvc();

    protected abstract String getIdMappingResultPath();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract FacetConfig getFacetConfig();

    protected abstract JobOperation getJobOperation();

    @Test
    void testIdMappingResultOnePage() throws Exception {
        // when
        Integer defaultPageSize = 5;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
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
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
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
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
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
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size))
                                        .param("cursor", cursor));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
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
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", "0"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testIdMappingResultWithNegativeSize() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
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
                getMockMvc()
                        .perform(
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

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void testGetResultsAllContentType(MediaType mediaType) throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, mediaType);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    @ParameterizedTest(name = "[{index}] sortFieldName {0} desc")
    @MethodSource("getAllSortFields")
    void testIdMappingWithAllAvailableSortFields(String sortField) throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("sort", sortField + " desc"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testIdMappingResultWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {

        assertThat(name, notNullValue());
        assertThat(paths, notNullValue());
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("fields", name));

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)));
        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*].to." + path;
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    @Test
    void searchFacetsWithIncorrectValuesReturnBadRequest() throws Exception {

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
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("facets", facetName)
                                        .param("size", "0"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets.*.name", contains(facetName)))
                .andExpect(jsonPath("$.facets[0].values.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets[0].values.*.count", hasItem(greaterThan(0))));
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType()).getReturnFields()
                .stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }

    protected Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType());
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    protected Stream<Arguments> getContentTypes() {
        return super.getContentTypes(getIdMappingResultPath());
    }

    private Stream<Arguments> getAllFacets() {
        return getFacetConfig().getFacetNames().stream().map(Arguments::of);
    }
}
