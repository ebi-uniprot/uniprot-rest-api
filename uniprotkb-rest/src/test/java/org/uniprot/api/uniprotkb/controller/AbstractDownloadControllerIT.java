package org.uniprot.api.uniprotkb.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.download.model.JobStatus;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractUniProtKBDownloadIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void runStarQuerySuccess() throws Exception {
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(query);
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    void runStarQueryMoreThanOnceShouldProcessOnlyOnceSuccess() throws Exception {
        String query = "*";
        String jobId = callRunAPIAndVerify(query);
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        String newJobId = callRunAPIAndVerify(query);
        Assertions.assertNotNull(newJobId);
        Assertions.assertEquals(jobId, newJobId);
        getAndVerifyDetails(query);
        verifyIdsAndResultFiles(jobId);
    }

    private Callable<JobStatus> jobProcessed(String jobId) {
        return () -> getJobStatus(jobId);
    }

    protected JobStatus getJobStatus(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/status/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.*", Matchers.notNullValue()));
        String status = response.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(status, "status should not be null");
        return JobStatus.valueOf(status);
    }

    protected String callRunAPIAndVerify(String query) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query);
        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.*", Matchers.notNullValue()));
        String jobId = response.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(jobId, "jobId should not be null");
        return jobId;
    }

    protected abstract String getDownloadAPIsBasePath();

    protected abstract void getAndVerifyDetails(String query);

    protected abstract void verifyIdsAndResultFiles(String jobId) throws IOException;
}
