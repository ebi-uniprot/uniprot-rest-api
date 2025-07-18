package org.uniprot.api.mapto.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.controller.ControllerITUtils.NO_CACHE_VALUE;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMapToControllerIT extends BaseMapToControllerIT {

    @Test
    void testSubmitJobAndVerifyGetStatus() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
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
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from", equalTo("UNIPROTKB")))
                .andExpect(jsonPath("$.to", equalTo("UNIREF")))
                .andExpect(jsonPath("$.query", equalTo("*:*")))
                .andExpect(jsonPath("$.includeIsoform", equalTo("false")));
        await().until(isJobFinished(jobId));
    }

    @Test
    void submitJobToFinishAndVerifyGetStatus() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
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
    void submitJobToErrorAndVerifyGetStatus() throws Exception {
        // when
        mockServerError();
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
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
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.ERROR.toString())))
                .andExpect(jsonPath("$.errors.size()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(50)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].message",
                                is("There is an error from the server side")));
    }

    @Test
    void submitJobToFinishAndVerifyGetDetails() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        ResultActions response = callGetJobDetails(jobId);
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.redirectURL", Matchers.endsWith(jobId)))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.from", equalTo("UNIPROTKB")))
                .andExpect(jsonPath("$.to", equalTo("UNIREF")))
                .andExpect(jsonPath("$.query", equalTo(query)))
                .andExpect(jsonPath("$.includeIsoform", equalTo("false")));
    }

    @Test
    void submitJobAndGetResultsBeforeItFinishes() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);

        // then
        ResultActions response = callGetJobResults(jobId, Map.of("query", "*"));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages[0]", containsString("not found")));
        await().until(isJobFinished(jobId));
    }

    @Test
    void runSameJobTwice_returnsSameJobId() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        ResultActions response = callRun(query);
        waitUntilTheJobIsAvailable(jobId);

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));
        await().until(isJobFinished(jobId));
    }

    @Test
    void getResultsRandomJobId() throws Exception {
        // when
        ResultActions response = callGetJobResults("jobId", Map.of("query", "*"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages[0]", containsString("not found")));
    }

    @Test
    void submitJobAndGetResultsExceedingTheEnrichmentLimit() throws Exception {
        // when
        String query = getQueryBeyondEnrichmentLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        ResultActions response = callGetJobResults(jobId, Map.of("query", "*"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("UniProt data enrichment is not supported")));
    }

    @Test
    void submitJobAndGetResultsExceedingTheToIdsLimit() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobErrored(jobId));
        ResultActions response = callGetJobStatus(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is("ERROR")))
                .andExpect(jsonPath("$.errors.size()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(40)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].message",
                                is("Number of target ids: 54 exceeds the allowed limit: 30")));
    }

    @Test
    void submitJobAndGetResultsExceedingTheFacetLimit() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        ResultActions response =
                callGetJobResults(jobId, Map.of("query", "*", "facets", getFacets(), "size", "0"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.warnings.size()", is(1)))
                .andExpect(jsonPath("$.warnings[0].code", is(20)))
                .andExpect(
                        jsonPath(
                                "$.warnings[0].message",
                                containsString(
                                        "Filters are not supported for mapping results with more than")))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void submitJobAndStreamResultsBeforeItFinishes() throws Exception {
        // when
        String query = "*:*";
        String jobId = callRunAPIAndVerify(query);

        // then
        MvcResult mvcResult =
                callGetJobResultsAsStream(jobId, Map.of("query", "*:*"), APPLICATION_JSON)
                        .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertThat(response.getContentAsString(), containsString("not found"));
        await().until(isJobFinished(jobId));
    }

    @Test
    void streamResultsRandomJobId() throws Exception {
        MvcResult mvcResult =
                callGetJobResultsAsStream("jobId", Map.of("query", "*:*"), APPLICATION_JSON)
                        .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertThat(response.getContentAsString(), containsString("not found"));
    }

    @Test
    void submitMapToJob_size() throws Exception {
        // when
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        String results = getJobResults(jobId, Map.of("query", "*", "size", "5"));
        verifyResultsWithSize(results);
    }

    @Test
    void submitMapToJob_filter() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        String results = getJobResults(jobId, getFilterQuery());
        verifyResultsWithFilter(results);
    }

    @Test
    void submitMapToJob_andResultsWithCursor() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        ResultActions response = callGetJobResults(jobId, Map.of("query", "*:*"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String resultsPage1 = response.andReturn().getResponse().getContentAsString();
        verifyResultsWithPaginationPageOne(resultsPage1);

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[1].split("=")[1];

        String resultsPage2 = getJobResults(jobId, Map.of("query", "*:*", "cursor", cursor));
        verifyResultsWithPaginationPageTwo(resultsPage2);
    }

    @Test
    void submitMapToJob_sort() throws Exception {
        String query = getQueryLessThanPageSize();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        String results = getJobResults(jobId, getSortQuery());
        verifyResultsWithSort(results);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getResultsContentTypes")
    void submitJobAndGetResults(MediaType mediaType) throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId)
                        .header(ACCEPT, mediaType.toString())
                        .param("query", "*:*");

        ResultActions response = getMockMvc().perform(requestBuilder);
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()));
    }

    @Test
    void submitJobAndGetResultsNonSupportedType() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        // then
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId).header(ACCEPT, "un/supported").param("query", "*:*");

        ResultActions response = getMockMvc().perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                equalTo(
                                        "Invalid request received. Requested media type/format not accepted: 'un/supported'.")));
    }

    @Test
    void submitMapToJobAndStreamResults() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        String results = getJobResultsAsStream(jobId, Map.of("query", "*:*"), APPLICATION_JSON);
        verifyResultsStream(results);
    }

    @Test
    void submitMapToJobAndStreamResultsWithDownloadFile() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        String results =
                getJobResultsAsStream(
                        jobId, Map.of("query", "*:*", "download", "true"), APPLICATION_JSON);
        verifyResultsStream(results);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getStreamContentTypes")
    void submitMapToJobAndStreamResults_differentFormats(MediaType mediaType) throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        getJobResultsAsStream(jobId, Map.of("query", "*:*"), mediaType);
    }

    @Test
    void submitMapToJobAndStreamResults_filter() throws Exception {
        String query = getQueryInLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        String results = getJobResultsAsStream(jobId, getFilterQuery(), APPLICATION_JSON);
        verifyResultsWithFilter(results);
    }

    @Test
    void submitJobAndStreamExceedingTheEnrichmentLimit() throws Exception {
        // when
        String query = getQueryBeyondEnrichmentLimits();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));

        // then
        MvcResult mvcResult =
                callGetJobResultsAsStream(jobId, getFilterQuery(), APPLICATION_JSON).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertThat(
                response.getContentAsString(),
                containsString("UniProt data enrichment is not supported"));
    }

    @Test
    void submitMapToJobAndStreamResults_sort() throws Exception {
        String query = getQueryLessThanPageSize();
        String jobId = callRunAPIAndVerify(query);
        waitUntilTheJobIsAvailable(jobId);
        await().until(isJobFinished(jobId));
        String results = getJobResultsAsStream(jobId, getSortQuery(), APPLICATION_JSON);
        verifyResultsWithSort(results);
    }

    protected abstract String getQueryLessThanPageSize();

    protected abstract String getQueryBeyondEnrichmentLimits();

    protected abstract void verifyResultsWithSize(String results);

    protected abstract String getFacets();

    protected abstract void verifyResultsWithSort(String results);

    protected abstract Map<String, String> getSortQuery();

    protected abstract Map<String, String> getFilterQuery();

    protected abstract void verifyResultsWithPaginationPageOne(String resultsJson);

    protected abstract void verifyResultsWithPaginationPageTwo(String resultsJson);

    protected abstract void verifyResultsWithFilter(String results);

    protected abstract void verifyResultsStream(String results);

    protected abstract int getTotalEntries();

    protected Callable<Boolean> isJobErrored(String jobId) {
        return () -> (getJobStatus(jobId).equals(JobStatus.ERROR));
    }

    protected String getJobResults(String jobId, Map<String, String> queryParams) throws Exception {
        ResultActions response = callGetJobResults(jobId, queryParams);
        // then
        response.andDo(log())
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
        return getMockMvc().perform(requestBuilder);
    }

    protected void getAndVerifyDetails(String jobId) throws Exception {
        // when
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
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
        return getMockMvc().perform(requestBuilder);
    }

    protected String getJobResultsAsStream(
            String jobId, Map<String, String> query, MediaType mediaType) throws Exception {
        MvcResult response = callGetJobResultsAsStream(jobId, query, mediaType).andReturn();
        boolean isDownload = Boolean.parseBoolean(query.getOrDefault("download", "false"));
        if (isDownload) {
            getMockMvc()
                    .perform(asyncDispatch(response))
                    .andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().exists("Content-Disposition"));
        } else {
            getMockMvc()
                    .perform(asyncDispatch(response))
                    .andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().doesNotExist("Content-Disposition"));
        }

        return response.getResponse().getContentAsString();
    }

    private Stream<Arguments> getStreamContentTypes() {
        return ControllerITUtils.getContentTypes(
                        getDownloadAPIsBasePath() + "/results/stream", requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }

    private Stream<Arguments> getResultsContentTypes() {
        return ControllerITUtils.getContentTypes(
                        getDownloadAPIsBasePath() + "/results/{jobId}",
                        requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }

    private ResultActions callGetJobResultsAsStream(
            String jobId, Map<String, String> queryParams, MediaType mediaType) throws Exception {
        String jobStreamUrl = getDownloadAPIsBasePath() + "/results/stream/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStreamUrl, jobId).header(ACCEPT, mediaType);
        queryParams.forEach(requestBuilder::param);
        return getMockMvc().perform(requestBuilder);
    }

    protected abstract void mockServerError();
}
