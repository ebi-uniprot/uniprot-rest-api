package uk.ac.ebi.uniprot.api.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author lgonzales
 */
@Slf4j
public abstract class AbstractBasicControllerIT {

    public enum SAVE_TYPE{
        ID_SUCCESS, ID_NOT_FOUND, ID_FILTER_SUCCESS,
        SEARCH_SUCCESS, SEARCH_NOT_FOUND
    }

    @Nested
    @DisplayName("/id endpoint Integration Tests")
    protected class GetByIdControllerIT {

        @Test
        protected void getIdCanReturnSuccessContentType(PathParameter idParameter, List<ContentTypeParam> contentTypes) throws Exception {
            // given
            assertThat(contentTypes, not(empty()));
            saveEntry(SAVE_TYPE.ID_SUCCESS);

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
            saveEntry(SAVE_TYPE.ID_NOT_FOUND);

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
                saveEntry(SAVE_TYPE.ID_FILTER_SUCCESS);

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
            saveEntry(SAVE_TYPE.SEARCH_SUCCESS);

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
            saveEntry(SAVE_TYPE.SEARCH_NOT_FOUND);

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
    }

    protected abstract void saveEntry(SAVE_TYPE entriesType);

    protected abstract MockMvc getMockMvc();

    protected abstract String getIdRequestPath();

    protected abstract String getSearchRequestPath();

}
