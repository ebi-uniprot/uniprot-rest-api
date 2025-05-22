package org.uniprot.api.mapto.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.controller.ControllerITUtils.NO_CACHE_VALUE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.store.search.SolrCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public abstract class MapToControllerIT {
    private static final String SOLR_SYSTEM_PROPERTIES = "solr-system.properties";
    public static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired protected SolrClient solrClient;
    @Autowired protected MapToJobRepository mapToJobRepository;
    @Autowired protected MockMvc mockMvc;
    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private MiniSolrCloudCluster cluster;

    private Path tempClusterDir;

    protected CloudSolrClient cloudSolrClient;

    @Container
    protected static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    public static void setUpThings(DynamicPropertyRegistry propertyRegistry) {
        Startables.deepStart(redisContainer).join();
        assertTrue(redisContainer.isRunning());
        System.setProperty("uniprot.redis.host", redisContainer.getHost());
        System.setProperty(
                "uniprot.redis.port", String.valueOf(redisContainer.getFirstMappedPort()));
        propertyRegistry.add("ALLOW_EMPTY_PASSWORD", () -> "yes");
    }

    @BeforeAll
    public void startCluster() throws Exception {
        Properties solrProperties = loadSolrProperties();
        String solrHome = solrProperties.getProperty("solr.home");
        tempClusterDir = Files.createTempDirectory("MiniSolrCloudCluster");
        System.setProperty(
                "solr.data.home", tempClusterDir.toString() + File.separator + "solrTestData");

        JettyConfig jettyConfig = JettyConfig.builder().setPort(0).stopAtShutdown(true).build();
        try {
            cluster = new MiniSolrCloudCluster(1, tempClusterDir, jettyConfig);
            Collection<TupleStreamTemplate> tupleStreamTemplates = getTupleStreamTemplates();
            cloudSolrClient = cluster.getSolrClient();
            for (TupleStreamTemplate tupleStreamTemplate : tupleStreamTemplates) {
                tupleStreamTemplate
                        .getStreamConfig()
                        .setZkHost(cluster.getZkServer().getZkAddress());
                ReflectionTestUtils.setField(tupleStreamTemplate, "streamFactory", null);
                ReflectionTestUtils.setField(tupleStreamTemplate, "streamContext", null);
                ReflectionTestUtils.setField(tupleStreamTemplate, "solrClient", cloudSolrClient);
            }
            updateFacetTupleStreamTemplate();

            for (SolrCollection solrCollection : getSolrCollections()) {
                String collection = solrCollection.name();
                Path configPath = Paths.get(solrHome + File.separator + collection + "/conf");
                cluster.uploadConfigSet(configPath, collection);
                CollectionAdminRequest.createCollection(collection, collection, 1, 1)
                        .process(cloudSolrClient);
            }
        } catch (Exception exc) {
            log.error("Failed to initialize a MiniSolrCloudCluster due to: " + exc, exc);
            throw exc;
        }
    }

    @Test
    void testSubmitJobAndVerifyGetStatus() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        ResultActions resultActions = callGetJobStatus(jobId);
        // then
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(
                        jsonPath(
                                "$.jobStatus",
                                oneOf(
                                        JobStatus.FINISHED.toString(),
                                        JobStatus.NEW.toString(),
                                        JobStatus.RUNNING.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void submitJobAndVerifyGetDetails() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from", equalTo("UNIPROTKB")))
                .andExpect(jsonPath("$.to", equalTo("UNIREF")))
                .andExpect(jsonPath("$.query", equalTo("*:*")))
                .andExpect(jsonPath("$.includeIsoform", equalTo("false")));
    }

    @Test
    void submitJobToFinishAndVerifyGetStatus() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
    }

    @Test
    void submitJobToFinishAndVerifyGetDetails() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.redirectURL", Matchers.endsWith(jobId)))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.from", equalTo("UNIPROTKB")))
                .andExpect(jsonPath("$.to", equalTo("UNIREF")))
                .andExpect(jsonPath("$.query", equalTo("*:*")))
                .andExpect(jsonPath("$.includeIsoform", equalTo("false")));
    }

    @Test
    void submitMapToJob_limit() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResults(jobId, Map.of("query", "*", "size", "5"));
        verifyResultsWithLimit(results);
    }

    protected abstract void verifyResultsWithLimit(String results);

    @Test
    void submitMapToJob_filter() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResults(jobId, getFilterQuery());
        verifyResultsWithFilter(results);
    }

    @Test
    void submitMapToJob_sort() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResults(jobId, getSortQuery());
        verifyResultsWithSort(results);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getResultsContentTypes")
    void submitJobAndGetResults(MediaType mediaType) throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId)
                        .header(ACCEPT, mediaType.toString())
                        .param("query", "*:*");

        ResultActions response = mockMvc.perform(requestBuilder);
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()));
    }

    @Test
    void submitMapToJobAndStreamResults() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResultsAsStream(jobId, Map.of("query", "*:*"), APPLICATION_JSON);
        verifyResults(results);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void submitMapToJobAndStreamResults_differentFormats(MediaType mediaType) throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        getJobResultsAsStream(jobId, Map.of("query", "*:*"), mediaType);
    }

    @Test
    void submitMapToJobAndStreamResults_filter() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResultsAsStream(jobId, getFilterQuery(), APPLICATION_JSON);
        verifyResultsWithFilter(results);
    }

    @Test
    void submitMapToJobAndStreamResults_sort() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(getTotalEntries())));
        String results = getJobResultsAsStream(jobId, getSortQuery(), APPLICATION_JSON);
        verifyResultsWithSort(results);
    }

    private Stream<Arguments> getContentTypes() {
        return ControllerITUtils.getContentTypes(
                        "/mapto" + getPath() + "/results/stream", requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }

    private Stream<Arguments> getResultsContentTypes() {
        return ControllerITUtils.getContentTypes(
                        "/mapto" + getPath() + "/results", requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }

    protected String getJobResultsAsStream(
            String jobId, Map<String, String> query, MediaType mediaType) throws Exception {
        MvcResult response = callGetJobResultsAsStream(jobId, query, mediaType).andReturn();

        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"));

        return response.getResponse().getContentAsString();
    }

    private ResultActions callGetJobResultsAsStream(
            String jobId, Map<String, String> queryParams, MediaType mediaType) throws Exception {
        String jobStreamUrl = getDownloadAPIsBasePath() + "/results/stream/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStreamUrl, jobId).header(ACCEPT, mediaType);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    protected abstract String getPath();

    protected abstract void verifyResultsWithSort(String results);

    protected abstract Map<String, String> getSortQuery();

    protected abstract Map<String, String> getFilterQuery();

    protected abstract void verifyResultsWithFilter(String results);

    protected abstract void verifyResults(String results);

    protected Callable<Boolean> isJobFinished(String jobId) {
        return () -> (getJobStatus(jobId).equals(JobStatus.FINISHED));
    }

    protected JobStatus getJobStatus(String jobId) throws Exception {
        ResultActions response = callGetJobStatus(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", Matchers.notNullValue()));
        String responseAsString = response.andReturn().getResponse().getContentAsString();
        String status = MAPPER.readTree(responseAsString).get("jobStatus").asText();
        assertNotNull(status, "status should not be null");
        return JobStatus.valueOf(status);
    }

    @NotNull
    protected ResultActions callGetJobStatus(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/status/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, APPLICATION_JSON);
        return mockMvc.perform(requestBuilder);
    }

    protected String getJobResults(String jobId, Map<String, String> queryParams) throws Exception {
        ResultActions response = callGetJobResults(jobId, queryParams);
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        return response.andReturn().getResponse().getContentAsString();
    }

    @NotNull
    protected ResultActions callGetJobResults(String jobId, Map<String, String> queryParams)
            throws Exception {
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId).header(ACCEPT, APPLICATION_JSON);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    protected void getAndVerifyDetails(String jobId) throws Exception {
        // when
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.redirectURL", Matchers.endsWith(jobId)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @NotNull
    protected ResultActions callGetJobDetails(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/details/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, APPLICATION_JSON);
        return mockMvc.perform(requestBuilder);
    }

    protected abstract String getDownloadAPIsBasePath();

    protected String callRunAPIAndVerify(String query, boolean includeIsoform) throws Exception {

        ResultActions response = callPostJobStatus(query, includeIsoform);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", Matchers.notNullValue()));
        String contentAsString = response.andReturn().getResponse().getContentAsString();
        String jobId = MAPPER.readTree(contentAsString).get("jobId").asText();
        assertNotNull(jobId, "jobId should not be null");
        return jobId;
    }

    protected abstract List<SolrCollection> getSolrCollections();

    protected abstract Collection<TupleStreamTemplate> getTupleStreamTemplates();

    protected abstract Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates();

    @AfterEach
    void tearDown() {
        mapToJobRepository.deleteAll();
    }

    @AfterAll
    public void stopCluster() throws Exception {
        if (cloudSolrClient != null) {
            cloudSolrClient.close();
            cloudSolrClient = null;
        }
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
        mapToJobRepository.deleteAll();
        redisContainer.stop();

        // Delete tempDir content
        FileSystemUtils.deleteRecursively(tempClusterDir);
    }

    private Properties loadSolrProperties() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesStream =
                AbstractStreamControllerIT.class
                        .getClassLoader()
                        .getResourceAsStream(SOLR_SYSTEM_PROPERTIES);
        properties.load(propertiesStream);
        return properties;
    }

    private void updateFacetTupleStreamTemplate() {
        // update facet tuple for fields value for testing
        Collection<FacetTupleStreamTemplate> facetTupleStreamTemplates =
                getFacetTupleStreamTemplates();
        for (FacetTupleStreamTemplate facetTupleStreamTemplate : facetTupleStreamTemplates) {
            ReflectionTestUtils.setField(
                    facetTupleStreamTemplate,
                    "zookeeperHost",
                    cluster.getZkServer().getZkAddress());
            ReflectionTestUtils.setField(facetTupleStreamTemplate, "streamFactory", null);
            ReflectionTestUtils.setField(facetTupleStreamTemplate, "streamContext", null);
        }
    }

    protected ResultActions callPostJobStatus(String query, boolean includeIsoform)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, APPLICATION_JSON)
                        .param("query", query)
                        .param("includeIsoform", "" + includeIsoform);
        return this.mockMvc.perform(requestBuilder);
    }

    protected abstract int getTotalEntries();
}
