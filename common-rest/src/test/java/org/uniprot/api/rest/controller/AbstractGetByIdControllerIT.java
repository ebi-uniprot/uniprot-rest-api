package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
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
import org.uniprot.core.util.Utils;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/** @author lgonzales */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractGetByIdControllerIT {
    // initialize this field in each child class to verify the order of fields in json response. see
    // method initExpectedFieldsOrder
    // this field is static because needs to be accessed from static context
    protected static List<String> JSON_RESPONSE_FIELDS_IN_EXPECTED_ORDER;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        SolrTemplate template = new SolrTemplate(storeManager.getSolrClient(getStoreType()));
        template.afterPropertiesSet();
        ReflectionTestUtils.setField(getRepository(), "solrTemplate", template);
        JSON_RESPONSE_FIELDS_IN_EXPECTED_ORDER = new LinkedList<>();
        initExpectedFieldsOrder();
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
                response.andDo(print())
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
    void withValidResponseFieldsOrder(GetIdParameter idParameter) throws Exception {
        validIdReturnSuccess(idParameter);
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
                response.andDo(print())
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
                response.andDo(print())
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
        if (Utils.notNullOrEmpty(idParameter.getFields())) {

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
                    response.andDo(print())
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
        if (Utils.notNullOrEmpty(idParameter.getFields())) {

            checkParameterInput(idParameter);

            // when
            MockHttpServletRequestBuilder requestBuilder =
                    get(getIdRequestPath() + idParameter.getId())
                            .header(ACCEPT, MediaType.APPLICATION_JSON)
                            .param("fields", idParameter.getFields());

            ResultActions response = mockMvc.perform(requestBuilder);

            // then
            ResultActions resultActions =
                    response.andDo(print())
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
                    response.andDo(print())
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
                    response.andDo(print())
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

    protected abstract void initExpectedFieldsOrder();

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
        ControllerITUtils.verifyContentTypes(
                getIdRequestPath(),
                requestMappingHandlerMapping,
                contentTypeParam.getContentTypeParams());
    }
}
