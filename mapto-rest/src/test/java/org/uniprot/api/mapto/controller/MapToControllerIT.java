package org.uniprot.api.mapto.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.store.search.SolrCollection.*;

import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.mapto.MapToREST;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.util.UniRefAsyncDownloadUtils;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = {"offline"})
@ContextConfiguration(
        classes = {
            UniRefDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            MapToREST.class,
            ErrorHandlerConfig.class
        })
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest
class MapToControllerIT extends BaseMapToControllerIT {
    @SpyBean private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired private MockMvc mockMvc;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient uniProtKBSolrClient;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository,
                cloudSolrClient,
                uniProtKBSolrClient,
                uniProtKBStoreClient,
                taxRepository);
        UniProtKBAsyncDownloadUtils.saveEntries(cloudSolrClient, uniProtKBStoreClient, 20);
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniRefQueryRepository, cloudSolrClient, solrClient, uniRefStoreClient, 20, "P");
    }

    @Test
    void testStreamMapToEntryIds() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToStreamPath(), jobId).header(ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        ResultActions response = mockMvc.perform(asyncDispatch(mvcResult));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(12)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id",
                                hasItems(
                                        "UniRef50_P00002",
                                        "UniRef90_P00002",
                                        "UniRef100_P00002",
                                        "UniRef50_P00004",
                                        "UniRef90_P00004",
                                        "UniRef100_P00004",
                                        "UniRef50_P00001",
                                        "UniRef90_P00001",
                                        "UniRef100_P00001",
                                        "UniRef50_P00003",
                                        "UniRef90_P00003",
                                        "UniRef100_P00003")));
    }

    @Test
    void testGetMapToEntryIds() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id",
                                hasItems(
                                        "UniRef100_P00003",
                                        "UniRef90_P00003",
                                        "UniRef50_P00003",
                                        "UniRef50_P00004",
                                        "UniRef90_P00004",
                                        "UniRef100_P00004",
                                        "UniRef50_P00001",
                                        "UniRef90_P00001",
                                        "UniRef100_P00001",
                                        "UniRef100_P00002")));
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        MockHttpServletRequestBuilder requestBuilderNext =
                get(getMapToResultPath(), jobId)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("cursor", cursor);
        ResultActions responseNext = mockMvc.perform(requestBuilderNext);
        responseNext
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id", hasItems("UniRef90_P00002", "UniRef50_P00002")));
    }

    @Test
    void testGetMapToEntryIds_customPageSize() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("size", "11");
        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(11)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id",
                                hasItems(
                                        "UniRef100_P00003",
                                        "UniRef90_P00002",
                                        "UniRef100_P00002",
                                        "UniRef50_P00004",
                                        "UniRef90_P00004",
                                        "UniRef100_P00004",
                                        "UniRef50_P00001",
                                        "UniRef90_P00001",
                                        "UniRef100_P00001",
                                        "UniRef50_P00003",
                                        "UniRef90_P00003")));
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        MockHttpServletRequestBuilder requestBuilderNext = requestBuilder.param("cursor", cursor);
        response = mockMvc.perform(requestBuilderNext);
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id", hasItems("UniRef50_P00002")));
        linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertNull(linkHeader);
    }

    @Test
    void testGetMapToEntryIds_sendWrongCursor() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("cursor", "random");
        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results").doesNotExist());
    }

    @Test
    void testGetMapToAllEntryIds() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("size", "20");
        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(12)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].id",
                                hasItems(
                                        "UniRef50_P00002",
                                        "UniRef90_P00002",
                                        "UniRef100_P00002",
                                        "UniRef50_P00004",
                                        "UniRef90_P00004",
                                        "UniRef100_P00004",
                                        "UniRef50_P00001",
                                        "UniRef90_P00001",
                                        "UniRef100_P00001",
                                        "UniRef50_P00003",
                                        "UniRef90_P00003",
                                        "UniRef100_P00003")));
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, nullValue());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE
            })
    void testStreamMapToEntryIds_withDifferentMediaTypes(String mediaType) throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToStreamPath(), jobId)
                        .header(HttpHeaders.ACCEPT, mediaType)
                        .param("download", "true");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        ResultActions response = mockMvc.perform(asyncDispatch(mvcResult));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"mapto_")));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE
            })
    void testResultMapToEntryIds_withDifferentMediaTypes(String mediaType) throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId).header(HttpHeaders.ACCEPT, mediaType);
        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType));
    }

    @Test
    void testStreamMapToEntryIds_limitExceedJobNotFound() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToStreamPath(), jobId).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/mapto/stream/mPHeqb5eJX"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Resource not found"));
    }

    @Test
    void testResultsMapToEntryIds_limitExceedJobNotFound() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), jobId).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/mapto/results/mPHeqb5eJX"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Resource not found"));
    }

    @Test
    void testStreamMapToEntryIds_randomJobId() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToStreamPath(), "random").header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/mapto/stream/random"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Resource not found"));
    }

    @Test
    void testResultsMapToEntryIds_randomJobId() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getMapToResultPath(), "random").header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/mapto/results/random"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Resource not found"));
    }

    private String getMapToStreamPath() {
        return "/mapto/stream/{jobId}";
    }

    private String getMapToResultPath() {
        return "/mapto/results/{jobId}";
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(uniprot, uniref, taxonomy);
    }

    @Override
    protected String getQueryInLimits() {
        return "accession:(P00001  OR P00002 OR P00003 OR P00004)";
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getDownloadAPIsBasePath() {
        return UniProtKBUniRefMapToController.RESOURCE_PATH;
    }

    @Override
    protected Collection<TupleStreamTemplate> getTupleStreamTemplates() {
        return List.of();
    }

    @Override
    protected Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates() {
        return List.of();
    }
}
