package org.uniprot.api.rest.controller;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
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
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        SolrTemplate template = new SolrTemplate(storeManager.getSolrClient(getStoreType()));
        template.afterPropertiesSet();
        ReflectionTestUtils.setField(getRepository(), "solrTemplate", template);
    }

    @AfterEach
    void cleanData() {
        storeManager.cleanSolr(getStoreType());
    }

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired private MockMvc mockMvc;

    protected abstract DataStoreManager.StoreType getStoreType();

    protected abstract SolrCollection getSolrCollection();

    protected abstract SolrQueryRepository getRepository();

    protected abstract String getDownloadRequestPath();

    protected abstract void saveEntries(int numberOfEntries);

    protected DataStoreManager getStoreManager() {
        return storeManager;
    }

    @Test
    void downloadAll(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerifyOkRequest(queryParameter);
    }

    @Test
    void downloadLessThanDefaultBatchSize(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerifyOkRequest(queryParameter);
    }

    @Test
    void downloadDefaultBatchSize(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerifyOkRequest(queryParameter);
    }

    @Test
    void downloadMoreThanBatchSize(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerifyOkRequest(queryParameter);
    }

    @Test
    void downloadSizeLessThanZero(SearchParameter queryParameter) throws Exception {
        sendAndVerifyBadRequest(queryParameter);
    }

    @Test
    void downloadWithoutQuery(SearchParameter queryParameter) throws Exception {
        sendAndVerifyBadRequest(queryParameter);
    }

    @Test
    void downloadWithBadQuery(SearchParameter queryParameter) throws Exception {
        sendAndVerifyBadRequest(queryParameter);
    }

    private void sendAndVerifyOkRequest(SearchParameter queryParameter) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach((paramName, values) -> requestBuilder.param(paramName, values.get(0)));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    private void sendAndVerifyBadRequest(SearchParameter queryParameter) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadRequestPath()).header(ACCEPT, MediaType.APPLICATION_JSON);

        queryParameter
                .getQueryParams()
                .forEach((paramName, values) -> requestBuilder.param(paramName, values.get(0)));

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }
}
