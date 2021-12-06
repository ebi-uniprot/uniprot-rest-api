package org.uniprot.api.idmapping.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
import static org.uniprot.api.idmapping.controller.UniParcIdMappingResultsController.UNIPARC_ID_MAPPING_PATH;
import static org.uniprot.api.idmapping.controller.UniProtKBIdMappingResultsController.UNIPROTKB_ID_MAPPING_PATH;
import static org.uniprot.api.idmapping.controller.UniRefIdMappingResultsController.UNIREF_ID_MAPPING_PATH;
import static org.uniprot.api.rest.output.converter.ConverterConstants.COPYRIGHT_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIREF_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.XML_DECLARATION;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingBasicControllerIT extends AbstractStreamControllerIT {

    @Value("${id.mapping.max.to.ids.with.facets.count}")
    protected Integer maxIdsWithFacets;

    @Value("${id.mapping.max.from.ids.count}")
    protected Integer maxFromIdsAllowed;

    @Value("${id.mapping.max.to.ids.count}")
    protected Integer maxToIdsAllowed;

    protected abstract String getIdMappingResultPath();

    protected abstract JobOperation getJobOperation();

    protected abstract MockMvc getMockMvc();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract String getFieldValueForValidatedField(String searchField);

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

    @Test
    void testWithMoreThanAllowedMappedId() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCacheWithOneToManyMapping(this.maxFromIdsAllowed, JobStatus.FINISHED);
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, APPLICATION_JSON_VALUE);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                        .andExpect(
                                jsonPath(
                                        "$.messages.*",
                                        containsInAnyOrder(
                                                "Invalid request received. Maximum number of mapped ids supported is " + this.maxToIdsAllowed)));;
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

        ResultActions response = performRequest(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                        .andExpect(content().contentTypeCompatibleWith(mediaType));
        if (MediaType.APPLICATION_XML.equals(mediaType)) {
            resultActions.andExpect(getXMLHeaderMatcher());
            resultActions.andExpect(getXMLFooterMatcher());
        }
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
        response.andDo(print())
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
                performRequest(
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

    @Test
    void testSearchWithoutFieldName() throws Exception {
        // when
        String searchQuery = getDefaultSearchQuery();
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId())
                        .param("query", searchQuery)
                        .header(ACCEPT, APPLICATION_JSON_VALUE);
        ResultActions response = performRequest(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)));
    }

    @Test
    void testSearchWithoutFieldNameWithEmptyResult() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId())
                        .param("query", "INVALIDVALUE")
                        .header(ACCEPT, APPLICATION_JSON_VALUE);
        ResultActions response = performRequest(requestBuilder);

        // then
        response.andDo(log())
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
                performRequest(
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

    protected ResultActions performRequest(MockHttpServletRequestBuilder requestBuilder)
            throws Exception {
        return getMockMvc().perform(requestBuilder);
    }

    protected String getDefaultSearchQuery() {
        return "*";
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

    private ResultMatcher getXMLHeaderMatcher() {
        ResultMatcher xmlHeaderMatcher;
        String resultPath = getIdMappingResultPath();
        if (resultPath.contains(UNIPROTKB_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(startsWith(XML_DECLARATION + UNIPROTKB_XML_SCHEMA));
        } else if (resultPath.contains(UNIREF_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(startsWith(XML_DECLARATION + UNIREF_XML_SCHEMA));
        } else if (resultPath.contains(UNIPARC_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(startsWith(XML_DECLARATION + UNIPARC_XML_SCHEMA));
        } else {
            xmlHeaderMatcher = content().string(startsWith("INVALID"));
        }
        return xmlHeaderMatcher;
    }

    private ResultMatcher getXMLFooterMatcher() {
        ResultMatcher xmlHeaderMatcher;
        String resultPath = getIdMappingResultPath();
        if (resultPath.contains(UNIPROTKB_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(endsWith(COPYRIGHT_TAG + UNIPROTKB_XML_CLOSE_TAG));
        } else if (resultPath.contains(UNIREF_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(not(containsStringIgnoringCase("<copyright>")));
        } else if (resultPath.contains(UNIPARC_ID_MAPPING_PATH)) {
            xmlHeaderMatcher = content().string(endsWith(COPYRIGHT_TAG + UNIPARC_XML_CLOSE_TAG));
        } else {
            xmlHeaderMatcher = content().string(startsWith("INVALID"));
        }
        return xmlHeaderMatcher;
    }
}
