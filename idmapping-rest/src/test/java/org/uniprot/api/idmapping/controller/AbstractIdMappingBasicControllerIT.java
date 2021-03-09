package org.uniprot.api.idmapping.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingBasicControllerIT extends AbstractStreamControllerIT {

    protected abstract String getIdMappingResultPath();

    protected abstract JobOperation getJobOperation();

    protected abstract MockMvc getMockMvc();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract String getFieldValueForValidatedField(String searchField);

    // ---------------------------------------------------------------------------------
    // -------------------------------- PAGINATION TEST --------------------------------
    // ---------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------
    // -------------------------------- JOB TESTS --------------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void testIdMappingWithWrongJobId() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), "WRONG_JOB_ID")
                                        .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void testIdMappingWithUnmappedIds() throws Exception {
        // when
        List<String> unmappedIds = List.of("UnMappedId1", "UnMappedId2");
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        job.getIdMappingResult().setUnmappedIds(unmappedIds);

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(5)))
                .andExpect(jsonPath("$.failedIds", contains("UnMappedId1", "UnMappedId2")));
    }

    @Test
    void testIdMappingWithJobIdWithErrorStatus() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(JobStatus.ERROR);

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }
    // ---------------------------------------------------------------------------------
    // -------------------------------- CONTENT TYPES ----------------------------------
    // ---------------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void testGetResultsAllContentType(MediaType mediaType) throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, mediaType);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    // ---------------------------------------------------------------------------------
    // -------------------------------- SORT -------------------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void searchSortWithIncorrectValuesReturnBadRequest() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "*:*")
                                        .param(
                                                "sort",
                                                "invalidField desc,invalidField1 invalidSort1")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid sort field order 'invalidsort1'. Expected asc or desc",
                                        "Invalid sort field 'invalidfield1'",
                                        "Invalid sort field 'invalidfield'")));
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
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    // ---------------------------------------------------------------------------------
    // -------------------------------- SEARCH FIELDS ----------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void testIdMappingWithInvalidQueryFormatReturnBadRequest() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param(
                                                "query",
                                                "invalidfield(:invalidValue AND :invalid:10)")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }

    @Test
    void testIdMappingWithInvalidFieldNameReturnBadRequest() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param(
                                                "query",
                                                "invalidfield:invalidValue OR invalidfield2:invalidValue2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalidfield' is not a valid search field",
                                        "'invalidfield2' is not a valid search field")));
    }

    @ParameterizedTest(name = "[{index}] search fieldName {0}")
    @MethodSource("getAllSearchFields")
    void searchCanSearchWithAllSearchFields(String searchField) throws Exception {
        assertThat(searchField, notNullValue());

        // when
        String fieldValue = getFieldValueForField(searchField);
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", searchField + ":" + fieldValue)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @Test
    void testSearchWithoutFieldName() throws Exception {
        // when
        String searchQuery = getDefaultSearchQuery();
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", searchQuery)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @Test
    void testSearchWithoutFieldNameWithEmptyResult() throws Exception {
        // when
        String searchQuery = UUID.randomUUID().toString();
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", searchQuery)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    // ---------------------------------------------------------------------------------
    // -------------------------------- RETURN FIELDS ----------------------------------
    // ---------------------------------------------------------------------------------

    @Test
    void testIdMappingResultWithInvalidReturnedFields() throws Exception {
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "*:*")
                                        .param("fields", "invalidField, otherInvalid")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalidField'",
                                        "Invalid fields parameter value 'otherInvalid'")));
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
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)));
        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*].to." + path;
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    protected String getDefaultSearchQuery() {
        return "*";
    }

    private Stream<Arguments> getAllSearchFields() {
        return SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .map(Arguments::of);
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(getUniProtDataType());
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(getIdMappingResultPath());
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType()).getReturnFields()
                .stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
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
