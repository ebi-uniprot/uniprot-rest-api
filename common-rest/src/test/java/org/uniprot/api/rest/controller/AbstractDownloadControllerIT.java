package org.uniprot.api.rest.controller;

import static org.hamcrest.core.IsAnything.anything;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT {
    // FIXME move it to common class
    public static String SEARCH_ACCESSION1 =
            "DI-" + ThreadLocalRandom.current().nextLong(10000, 99999);
    public static String SEARCH_ACCESSION2 =
            "DI-" + ThreadLocalRandom.current().nextLong(10000, 99999);
    public static List<String> SORTED_ACCESSIONS =
            new ArrayList<>(Arrays.asList(SEARCH_ACCESSION1, SEARCH_ACCESSION2));

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

    protected abstract void saveEntry(String accession, long suffix);

    protected DataStoreManager getStoreManager() {
        return storeManager;
    }

    protected void testDownloadAll(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerify(queryParameter, HttpStatus.OK, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadLessThanDefaultBatchSize(SearchParameter queryParameter)
            throws Exception {
        // given
        saveEntries(500);
        sendAndVerify(queryParameter, HttpStatus.OK, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadDefaultBatchSize(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerify(queryParameter, HttpStatus.OK, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadMoreThanBatchSize(SearchParameter queryParameter) throws Exception {
        // given
        saveEntries(500);
        sendAndVerify(queryParameter, HttpStatus.OK, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadSizeLessThanZero(SearchParameter queryParameter) throws Exception {
        sendAndVerify(queryParameter, HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadWithoutQuery(SearchParameter queryParameter) throws Exception {
        sendAndVerify(queryParameter, HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
    }

    protected void testDownloadWithBadQuery(SearchParameter queryParameter) throws Exception {
        sendAndVerify(queryParameter, HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
    }

    protected void searchSuccessContentTypes(SearchContentTypeParam contentTypeParam)
            throws Exception {
        // given
        saveEntry(SEARCH_ACCESSION1, 10L);
        saveEntry(SEARCH_ACCESSION2, 20L);
        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            sendAndVerify(contentTypeParam, HttpStatus.OK, contentType);
        }
    }

    protected void searchBadRequestContentTypes(SearchContentTypeParam contentTypeParam)
            throws Exception {
        // when
        for (ContentTypeParam contentType : contentTypeParam.getContentTypeParams()) {
            sendAndVerify(contentTypeParam, HttpStatus.BAD_REQUEST, contentType);
        }
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        ResultActions resultActions = mockMvc.perform(builder);
        if (resultActions.andReturn().getRequest().isAsyncStarted()) {
            return mockMvc.perform(
                    asyncDispatch(
                            resultActions
                                    .andExpect(request().asyncResult(anything()))
                                    .andReturn()));
        } else {
            return resultActions;
        }
    }

    private void sendAndVerify(
            SearchParameter queryParameter, HttpStatus httpStatus, MediaType contentType)
            throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadRequestPath()).header(ACCEPT, contentType);

        queryParameter
                .getQueryParams()
                .forEach((paramName, values) -> requestBuilder.param(paramName, values.get(0)));

        ResultActions response = perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(httpStatus.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, contentType.toString()));

        for (ResultMatcher resultMatcher : queryParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    private void sendAndVerify(
            SearchContentTypeParam contentTypeParam,
            HttpStatus httpStatus,
            ContentTypeParam contentType)
            throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadRequestPath())
                        .param("query", contentTypeParam.getQuery())
                        .header(ACCEPT, contentType.getContentType());

        ResultActions response = perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(httpStatus.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                contentType.getContentType().toString()));

        for (ResultMatcher resultMatcher : contentType.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }
}
