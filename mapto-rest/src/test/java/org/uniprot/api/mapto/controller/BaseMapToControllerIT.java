package org.uniprot.api.mapto.controller;

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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.service.PostgresTestContainer;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.search.SolrCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseMapToControllerIT extends AbstractStreamControllerIT {
    protected static final String SERVER_ERROR = "There is an error from the server side";
    public static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired protected MapToJobRepository mapToJobRepository;
    @Autowired protected RequestMappingHandlerMapping requestMappingHandlerMapping;

    @DynamicPropertySource
    public static void setUpThings(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgresContainer = PostgresTestContainer.getInstance();
        assertTrue(postgresContainer.isRunning());
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @AfterEach
    void tearDown() {
        mapToJobRepository.deleteAll();
    }

    protected abstract Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates();

    protected abstract List<SolrCollection> getSolrCollections();

    protected abstract String getQueryInLimits();

    protected String callRunAPIAndVerify(String query) throws Exception {

        ResultActions response = callRun(query);

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

    protected abstract MockMvc getMockMvc();

    protected void waitUntilTheJobIsAvailable(String jobId) {
        await().until(() -> mapToJobRepository.existsByJobId(jobId));
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
}
