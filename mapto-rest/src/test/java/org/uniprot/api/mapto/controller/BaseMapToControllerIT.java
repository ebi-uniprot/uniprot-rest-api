package org.uniprot.api.mapto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
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
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.search.SolrCollection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public abstract class BaseMapToControllerIT {
    private static final String SOLR_SYSTEM_PROPERTIES = "solr-system.properties";
    protected static final String SERVER_ERROR = "There is an error from the server side";
    public static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired protected SolrClient solrClient;
    @Autowired protected MapToJobRepository mapToJobRepository;
    @Autowired protected RequestMappingHandlerMapping requestMappingHandlerMapping;

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

    protected abstract Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates();

    protected abstract Collection<TupleStreamTemplate> getTupleStreamTemplates();

    protected abstract List<SolrCollection> getSolrCollections();

    protected abstract String getQueryInLimits();

    protected String callRunAPIAndVerify(String query) throws Exception {

        ResultActions response = callRun(query);

        return verifyRunResponseAndGetJobId(response);
    }

    protected String callRunAPIAndVerify(String query, boolean includeIsoform) throws Exception {

        ResultActions response = callRun(query, includeIsoform);

        return verifyRunResponseAndGetJobId(response);
    }

    private static @NotNull String verifyRunResponseAndGetJobId(ResultActions response)
            throws Exception {
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", Matchers.notNullValue()));
        String contentAsString = response.andReturn().getResponse().getContentAsString();
        String jobId = MAPPER.readTree(contentAsString).get("jobId").asText();
        assertNotNull(jobId, "jobId should not be null");
        return jobId;
    }

    protected ResultActions callRun(String query) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, APPLICATION_JSON)
                        .param("query", query);
        return getMockMvc().perform(requestBuilder);
    }

    protected ResultActions callRun(String query, boolean includeIsoform) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, APPLICATION_JSON)
                        .param("query", query)
                        .param("includeIsoform", String.valueOf(includeIsoform));
        return getMockMvc().perform(requestBuilder);
    }

    protected abstract MockMvc getMockMvc();

    protected void waitUntilTheJobIsAvailable(String jobId) {
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().atLeast(50, TimeUnit.MILLISECONDS);
    }

    protected Callable<Boolean> isJobFinished(String jobId) {
        return () ->
                (getJobStatus(jobId).equals(JobStatus.FINISHED)
                        || getJobStatus(jobId).equals(JobStatus.ERROR));
    }

    @NotNull
    protected ResultActions callGetJobStatus(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/status/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, APPLICATION_JSON);
        return getMockMvc().perform(requestBuilder);
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

    protected abstract String getDownloadAPIsBasePath();

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

    private Properties loadSolrProperties() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesStream =
                AbstractStreamControllerIT.class
                        .getClassLoader()
                        .getResourceAsStream(SOLR_SYSTEM_PROPERTIES);
        properties.load(propertiesStream);
        return properties;
    }
}
