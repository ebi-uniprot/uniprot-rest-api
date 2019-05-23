package uk.ac.ebi.uniprot.api.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author lgonzales
 */
@Slf4j
public abstract class AbstractBasicControllerIT {

    public enum SAVE_CONTEXT {
        ID_SUCCESS, ID_NOT_FOUND, ID_FILTER_SUCCESS,
        SEARCH_SUCCESS, SEARCH_NOT_FOUND
    }

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * This test will verify if all content types were configured in ContentTypeParamParamResolver for success
     */
    @Test
    protected void configuredAllContentTypeForSearchSuccessEndpoint(List<ContentTypeParam> contentTypes){
        verifyContentTypes(contentTypes);
    }

    /**
     * This test will verify if all content types were configured in ContentTypeParamParamResolver for not found
     */
    @Test
    protected void configuredAllContentTypeForSearchNotFoundEndpoint(List<ContentTypeParam> contentTypes){
        verifyContentTypes(contentTypes);
    }

    /**
     * This test will verify if all content types were configured in ContentTypeParamParamResolver for bad request
     */
    @Test
    protected void configuredAllContentTypeForSearchBadRequestEndpoint(List<ContentTypeParam> contentTypes){
        verifyContentTypes(contentTypes);
    }
    private void verifyContentTypes(List<ContentTypeParam> contentTypes) {
        assertThat(contentTypes,notNullValue());
        assertThat(contentTypes,not(empty()));
        List<MediaType> mediaTypes = contentTypes.stream()
                .map(ContentTypeParam::getContentType)
                .collect(Collectors.toList());

        RequestMappingInfo mappingInfo = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .filter(requestMappingInfo -> requestMappingInfo.getPatternsCondition().getPatterns().contains(getSearchRequestPath()))
                .findFirst().orElse(null);

        assertThat(mappingInfo,notNullValue());
        assertThat(mappingInfo.getProducesCondition().getProducibleMediaTypes(),contains(mediaTypes));
    }

    @Nested
    @DisplayName("/id endpoint Integration Tests")
    protected class GetByIdControllerIT {

        @Test
        protected void getIdCanReturnSuccessContentType(PathParameter idParameter, List<ContentTypeParam> contentTypes) throws Exception {
            // given
            assertThat(contentTypes, not(empty()));
            saveEntry(SAVE_CONTEXT.ID_SUCCESS);

            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + idParameter.getPathParam())
                        .header(ACCEPT, contentType.getContentType());

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void getIdCanReturnBadRequestContentType(PathParameter idParameter, List<ContentTypeParam> contentTypes) throws Exception {
            // when
            assertThat(contentTypes, not(empty()));
            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + idParameter.getPathParam())
                        .header(ACCEPT, contentType.getContentType());

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void getIdCanReturnNotFoundRequest(PathParameter idParameter, List<ContentTypeParam> contentTypes) throws Exception {
            // when
            assertThat(contentTypes, not(empty()));
            saveEntry(SAVE_CONTEXT.ID_NOT_FOUND);

            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + idParameter.getPathParam())
                        .header(ACCEPT, contentType.getContentType());

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void getIdCanFilterFieldsEntryReturnSuccess(PathParameter idParameter, QueryParameter queryParameter) throws Exception {
            if (queryParameter.getQueryParams() != null && !queryParameter.getQueryParams().isEmpty()) {
                //when
                saveEntry(SAVE_CONTEXT.ID_FILTER_SUCCESS);

                MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + idParameter.getPathParam())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

                queryParameter.getQueryParams().forEach((paramName, values) -> {
                    requestBuilder.param(paramName, values.toArray(new String[0]));
                });

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

                for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            } else {
                log.info("Filter fields are not being tested, I am assuming that this is not a supported feature for this endpoint");
            }
        }

        @Test
        protected void getIdInvalidFilterFieldsEntryReturnBadRequest(PathParameter idParameter, QueryParameter queryParameter) throws Exception {
            if (queryParameter.getQueryParams() != null && !queryParameter.getQueryParams().isEmpty()) {
                MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + idParameter.getPathParam())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

                queryParameter.getQueryParams().forEach((paramName, values) -> {
                    requestBuilder.param(paramName, values.toArray(new String[0]));
                });

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

                for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            } else {
                log.info("Filter fields are not being tested, I am assuming that this is not a supported feature for this endpoint");
            }
        }

    }

    @Nested
    @DisplayName("/search Integration Tests")
    protected class SearchControllerIT {

        @Test
        protected void searchCanReturnSuccessContentType(QueryParameter queryParameter, List<ContentTypeParam> contentTypes) throws Exception {
            // given
            assertThat(contentTypes,is(notNullValue()));
            assertThat(contentTypes.size(), greaterThan(0));
            saveEntry(SAVE_CONTEXT.SEARCH_SUCCESS);

            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getSearchRequestPath())
                        .header(ACCEPT, contentType.getContentType());

                queryParameter.getQueryParams().forEach((paramName, values) -> {
                    requestBuilder.param(paramName, values.toArray(new String[0]));
                });

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void searchCanReturnNotFoundRequest(QueryParameter queryParameter, List<ContentTypeParam> contentTypes) throws Exception {

            assertThat(contentTypes,is(notNullValue()));
            assertThat(contentTypes.size(), greaterThan(0));
            saveEntry(SAVE_CONTEXT.SEARCH_NOT_FOUND);

            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getSearchRequestPath())
                        .header(ACCEPT, contentType.getContentType());

                queryParameter.getQueryParams().forEach((paramName, values) -> {
                    requestBuilder.param(paramName, values.toArray(new String[0]));
                });

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void searchCanReturnBadRequestContentType(QueryParameter queryParameter, List<ContentTypeParam> contentTypes) throws Exception {

            assertThat(contentTypes,is(notNullValue()));
            assertThat(contentTypes.size(), greaterThan(0));
            for (ContentTypeParam contentType : contentTypes) {
                // when
                MockHttpServletRequestBuilder requestBuilder = get(getSearchRequestPath())
                        .header(ACCEPT, contentType.getContentType());

                queryParameter.getQueryParams().forEach((paramName, values) -> {
                    requestBuilder.param(paramName, values.toArray(new String[0]));
                });

                ResultActions response = getMockMvc().perform(requestBuilder);

                // then
                ResultActions resultActions = response.andDo(print())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType.getContentType().toString()));

                for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
                for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                    resultActions.andExpect(resultMatcher);
                }
            }
        }

        @Test
        protected void searchWithoutQueryReturnBadRequest() throws Exception{
            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",contains("query is a required parameter")));
        }

        @Test
        protected void searchAllowQueryAllDocumentsReturnSuccess() throws Exception {
            // given
            saveEntry(SAVE_CONTEXT.SEARCH_SUCCESS);

            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","*:*")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()",greaterThan(0)));
        }

        @Test
        protected void searchQueryWithInvalidQueryFormatReturnBadRequest() throws Exception {

            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","invalidfield(:invalidValue AND :invalid:10)")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",contains("query parameter has an invalid syntax")));
        }

        @Test
        public void searchQueryWithInvalidFieldNameReturnBadRequest() throws Exception {
            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","invalidfield:invalidValue OR invalidfield2:invalidValue2")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",
                            containsInAnyOrder("'invalidfield' is not a valid search field",
                                    "'invalidfield2' is not a valid search field")));
        }


        //TODO: more tests for query validations with parameters
        /* @Test
        protected void searchAllowWildcardQueryAllDocuments(QueryParameter queryParameter) throws Exception {
        }
        protected void searchQueryWithInvalidTypeQueryReturnBadRequest(QueryParameter queryParameter) throws Exception {
        }

        protected void searchQueryWithInvalidValueQueryReturnBadRequest(QueryParameter queryParameter) throws Exception {
        }



        @Test
        public void searchSortWithCorrectValuesReturnSuccess(QueryParameter queryParameter) throws Exception {
        */

        @Test
        public void searchSortWithIncorrectValuesReturnBadRequest() throws Exception {
            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","*:*")
                            .param("sort","invalidField desc,invalidField1 invalidSort1")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));

            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",
                            containsInAnyOrder("Invalid sort field order invalidsort1. Expected asc or desc",
                                    "Invalid sort field invalidfield",
                                    "Invalid sort field invalidfield1")));
        }

/*
        //TODO: more tests for valid field
        @Test
        public void searchFieldsWithCorrectValuesReturnSuccess(QueryParameter queryParameter) throws Exception {
        }
 */
        @Test
        public void searchFieldsWithIncorrectValuesReturnBadRequest() throws Exception {
            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","*:*")
                            .param("fields","invalidField, otherInvalid")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",
                            containsInAnyOrder("Invalid fields parameter value 'invalidField'",
                                    "Invalid fields parameter value 'otherInvalid'")));
        }

        /*
        //TODO: more tests for valid facets
        @Test
        public void searchFacetsWithCorrectValuesReturnSuccess(QueryParameter queryParameter) throws Exception {
        }

        @Test
        public void searchFacetsForXMLFormatReturnBadRequest(QueryParameter queryParameter) throws Exception {

        }
 */

        @Test
        public void searchFacetsWithIncorrectValuesReturnBadRequest() throws Exception {

            // when
            ResultActions response = getMockMvc().perform(
                    get(getSearchRequestPath())
                            .param("query","*:*")
                            .param("facets","invalid, invalid2")
                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            // then
            response.andDo(print())
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.messages.*",containsInAnyOrder(
                                    startsWith("Invalid facet name 'invalid'. Expected value can be "),
                                    startsWith("Invalid facet name 'invalid2'. Expected value can be "))));
        }

    }

    protected abstract void saveEntry(SAVE_CONTEXT context);

    protected abstract MockMvc getMockMvc();

    protected abstract String getIdRequestPath();

    protected abstract String getSearchRequestPath();

}
