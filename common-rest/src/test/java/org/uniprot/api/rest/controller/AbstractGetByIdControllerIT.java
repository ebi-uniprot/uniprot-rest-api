package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/** @author lgonzales */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractGetByIdControllerIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        ReflectionTestUtils.setField(
                getRepository(), "solrClient", storeManager.getSolrClient(getStoreType()));
    }

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired private MockMvc mockMvc;

    @Test
    void validIdReturnSuccess(GetIdParameter idParameter) throws Exception {
        checkParameterInput(idParameter);
        // given
        saveEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath() + idParameter.getId())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void invalidIdReturnBadRequest(GetIdParameter idParameter) throws Exception {
        checkParameterInput(idParameter);
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath() + idParameter.getId())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void nonExistentIdReturnFoundRequest(GetIdParameter idParameter) throws Exception {
        checkParameterInput(idParameter);
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath() + idParameter.getId())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void withFilterFieldsReturnSuccess(GetIdParameter idParameter) throws Exception {
        assertThat(idParameter, notNullValue());
        if (Utils.notNullNotEmpty(idParameter.getFields())) {

            checkParameterInput(idParameter);

            // when
            saveEntry();

            MockHttpServletRequestBuilder requestBuilder =
                    get(getIdRequestPath() + idParameter.getId())
                            .header(ACCEPT, MediaType.APPLICATION_JSON)
                            .param("fields", idParameter.getFields());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.OK.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    MediaType.APPLICATION_JSON_VALUE));

            for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        } else {
            log.info(
                    "Filter fields are not being tested, I am assuming that this is not a supported feature for this endpoint");
        }
    }

    @Test
    void withInvalidFilterFieldsReturnBadRequest(GetIdParameter idParameter) throws Exception {
        if (Utils.notNullNotEmpty(idParameter.getFields())) {

            checkParameterInput(idParameter);

            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getIdRequestPath() + idParameter.getId())
                            .header(ACCEPT, MediaType.APPLICATION_JSON)
                            .param("fields", idParameter.getFields());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    MediaType.APPLICATION_JSON_VALUE));

            for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        } else {
            log.info(
                    "Filter fields are not being tested, I am assuming that this is not a supported feature for this endpoint");
        }
    }

    @Test
    void idSuccessContentTypes(GetIdContentTypeParam contentTypeParam) throws Exception {
        // given
        saveEntry();

        checkIdContentTypeParameterInput(contentTypeParam);

        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getIdRequestPath() + contentTypeParam.getId())
                            .header(ACCEPT, contentType.getContentType());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.OK.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    contentType.getContentType().toString()));

            for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        }
    }

    @Test
    void idBadRequestContentTypes(GetIdContentTypeParam contentTypeParam) throws Exception {
        checkIdContentTypeParameterInput(contentTypeParam);

        // when
        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getIdRequestPath() + contentTypeParam.getId())
                            .header(ACCEPT, contentType.getContentType());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(log())
                            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    contentType.getContentType().toString()));

            for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
        }
    }

    // -----------------------------------------------
    // TEST DEFAULT CONTENT TYPE AND ENTRY EXTENSION
    // -----------------------------------------------

    // if no content type is provided, use json
    @Test
    void idWithoutContentTypeMeansUseDefaultContentType(GetIdParameter idParameter)
            throws Exception {
        checkParameterInput(idParameter);

        // given
        saveEntry();

        // when
        ResultActions response = mockMvc.perform(get(getIdRequestPath() + idParameter.getId()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, DEFAULT_MEDIA_TYPE_VALUE));
    }

    // if extension for content type present for fetching an entry, use it
    @Test
    void idWithExtensionMeansUseThatContentType(GetIdParameter idParameter) throws Exception {
        checkParameterInput(idParameter);

        // given
        saveEntry();

        // when
        String extension = "json";
        ResultActions response =
                mockMvc.perform(get(getIdRequestPath() + idParameter.getId() + "." + extension));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.getMediaTypeForFileExtension(extension)
                                                .toString()));
    }

    protected DataStoreManager getStoreManager() {
        return storeManager;
    }

    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    protected abstract DataStoreManager.StoreType getStoreType();

    protected abstract SolrCollection getSolrCollection();

    protected abstract SolrQueryRepository getRepository();

    protected abstract void saveEntry();

    protected abstract String getIdRequestPath();

    private void checkParameterInput(GetIdParameter idParameter) {
        assertThat(idParameter, notNullValue());
        assertThat(idParameter.getId(), notNullValue());
        assertThat(idParameter.getId(), not(isEmptyOrNullString()));
        assertThat(idParameter.getResultMatchers(), notNullValue());
        assertThat(idParameter.getResultMatchers(), not(emptyIterable()));
    }

    private void checkIdContentTypeParameterInput(GetIdContentTypeParam contentTypeParam) {
        assertThat(contentTypeParam, notNullValue());
        assertThat(contentTypeParam.getContentTypeParams(), notNullValue());
        assertThat(contentTypeParam.getContentTypeParams(), not(empty()));
        ControllerITUtils.verifyIdContentTypes(
                getIdRequestPath() + "{",
                requestMappingHandlerMapping,
                contentTypeParam.getContentTypeParams());
    }
}
