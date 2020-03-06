package org.uniprot.api.rest.controller;

import static org.hamcrest.core.IsAnything.anything;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT {
    public static final Integer ENTRY_COUNT = 500;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        SolrTemplate template = new SolrTemplate(storeManager.getSolrClient(getStoreType()));
        template.afterPropertiesSet();
        ReflectionTestUtils.setField(getRepository(), "solrTemplate", template);
    }

    @BeforeEach
    public void setUpData() {
        saveEntries(ENTRY_COUNT);
    }

    @AfterEach
    public void cleanData() {
        storeManager.cleanStore(getStoreType());
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

    protected void sendAndVerify(DownloadParamAndResult paramAndResult, HttpStatus httpStatus)
            throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadRequestPath()).header(ACCEPT, paramAndResult.getContentType());

        paramAndResult
                .getQueryParams()
                .forEach((paramName, values) -> requestBuilder.param(paramName, values.get(0)));

        ResultActions response = perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(httpStatus.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                paramAndResult.getContentType().toString()));

        if (HttpStatus.OK.equals(httpStatus)) {
            for (ResultMatcher resultMatcher : paramAndResult.getResultMatchers()) {
                resultActions.andExpect(resultMatcher);
            }
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
}
