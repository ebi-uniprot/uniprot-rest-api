package org.uniprot.api.rest.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.download.TestUtils.uncompressFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.context.FileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractDownloadIT {
    public static ObjectMapper MAPPER = new ObjectMapper();

    protected abstract MockMvc getMockMvcObject();

    @Test
    protected void submitJobWithoutQueryFailure() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        ResultActions resultActions = callPostJobStatus(null, null, null, format.toString(), false);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.messages", contains("'query' is a required parameter")));
    }

    @Test
    protected void submitJobWithStarQuerySuccess() throws Exception {
        String query = "*:*";
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    protected void submitJobStarQueryMoreThanOnceShouldProcessOnlyOnceSuccess() throws Exception {
        String query = "*";
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().atMost(20, SECONDS).until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        Optional<DownloadJob> optJob1 = getDownloadJobRepository().findById(jobId);
        String newJobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        assertNotNull(newJobId);
        assertEquals(jobId, newJobId);
        Optional<DownloadJob> optJob2 = getDownloadJobRepository().findById(jobId);
        assertAll(() -> assertTrue(optJob1.isPresent()), () -> assertTrue(optJob2.isPresent()));
        assertEquals(optJob1.get(), optJob2.get());
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    protected void submitJobWithSort() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = submitJobWithQuery();
        String sort = submitJobWithSortFields();
        String jobId = callRunAPIAndVerify(query, null, sort, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobFinished(jobId));
        // then
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        List<String> returnIds = submitJobWithSortResultIds();
        Assertions.assertNotNull(ids);
        assertTrue(ids.containsAll(returnIds));
        // verify result file
        Path resultFilePath =
                Path.of(this.resultFolder + "/" + jobId + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, getResultIdStringToMatch());
        assertTrue(primaryAccessions.containsAll(returnIds));
    }

    @ParameterizedTest(name = "[{index}] format {0}")
    @MethodSource("getSupportedFormats")
    protected void submitJobAllFormat(String format) throws Exception {
        // when
        String query = getSubmitJobAllFormatQuery();
        String fields = getSubmitJobAllFormatQueryFields();
        String jobId = callRunAPIAndVerify(query, fields, null, format, false);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        if ("h5".equals(format) || "application/x-hdf5".equals(format)) {
            await().atMost(30, SECONDS).until(isJobRunning(jobId));
        } else {
            await().atMost(30, SECONDS).until(isJobFinished(jobId));
            getAndVerifyDetails(jobId);
        }
    }

    @Test
    protected void submitJobWithoutFormatDefaultsToJson() throws Exception {
        // when
        String query = submitJobWithQuery();
        String fields = submitJobWithoutFormatDefaultsToJsonGetField();
        String jobId = callRunAPIAndVerify(query, fields, null, null, false);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
    }

    @Test
    protected void submitJobUnsupportedFormatFailure() throws Exception {
        // when
        MediaType format = getUnsupportedFormat();
        String query = submitJobWithQuery();
        String fields = getSubmitJobUnsupportedFormatFailureFields();
        ResultActions response = callPostJobStatus(query, fields, null, format.toString(), false);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", Matchers.contains(getUnsupportedFormatErrorMsg())));
    }

    @Test
    protected void submitJobWithBadQuery() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "random:field";
        ResultActions response = callPostJobStatus(query, null, null, format.toString(), false);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath("$.messages.*", contains("'random' is not a valid search field")));
    }

    @Test
    protected void submitJobWithBadField() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = submitJobWithQuery();
        String fields = "something,else";

        ResultActions response = callPostJobStatus(query, fields, null, format.toString(), false);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'else'",
                                        "Invalid fields parameter value 'something'")));
    }

    @Test
    protected void submitJobWithBadSort() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = submitJobWithQuery();
        String sort = "something desc";

        ResultActions response = callPostJobStatus(query, null, sort, format.toString(), false);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder("Invalid sort field 'something'")));
    }

    @Test
    protected void getStatusReturnsError() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String errMsg = "this is a friendly error message";
        DownloadJob job = builder.id(jobId).status(JobStatus.ERROR).error(errMsg).build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.ERROR.toString())))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code", is(PredefinedAPIStatus.SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    protected void getStatusReturnsNew() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        DownloadJob job = builder.id(jobId).status(JobStatus.NEW).build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.NEW.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    protected void getStatusForUnknownJobId() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ResultActions response = callGetJobStatus(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", contains("Resource not found")));
    }

    @Test
    protected void getDetailsForRunningJob() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String query = "my:query";
        DownloadJob job = builder.id(jobId).status(JobStatus.RUNNING).query(query).build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobDetails(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.query", is(query)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    protected void getDetailsForUnknownJobId() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", contains("Resource not found")));
    }

    @Test
    protected void getDetailsWithErroredJob() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String errMsg = "this is a friendly error message";
        String query = "sample:query";
        String fields = "sample,fields";
        String sort = "sample sort";
        DownloadJob job =
                builder.id(jobId)
                        .query(query)
                        .sort(sort)
                        .fields(fields)
                        .status(JobStatus.ERROR)
                        .error(errMsg)
                        .format(APPLICATION_JSON_VALUE)
                        .build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobDetails(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.query", is(query)))
                .andExpect(jsonPath("$.fields", is(fields)))
                .andExpect(jsonPath("$.sort", is(sort)))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code", is(PredefinedAPIStatus.SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    protected void runQueryWhichReturnsEmptyResult() throws Exception {
        String query = getRunQueryWhichReturnsEmptyResult();
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());

        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(ids.isEmpty());
        // verify result file
        Path resultFilePath =
                Path.of(this.resultFolder + "/" + jobId + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFilePath = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFilePath);
        String resultsJson = Files.readString(unzippedFilePath);
        List<String> results = JsonPath.read(resultsJson, "$.results");
        Assertions.assertAll(() -> assertNotNull(results), () -> assertTrue(results.isEmpty()));
    }

    @Test
    protected void runJobWithFieldsTSVAndVerify() throws Exception {
        // when
        String query = getQueryForJSONAndTSVRunJobWithFields();
        String fields = getQueryFieldsForJSONAndTSVRunJobWithFields();
        String jobId = callRunAPIAndVerify(query, fields, null, "tsv", false);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String tsvString = Files.readString(unzippedFile);
        String[] rows = tsvString.split("\n");
        Assertions.assertEquals(13, rows.length);
        Assertions.assertEquals(getRunJobHeaderWithFieldsTSV(), rows[0]);
    }

    protected JobStatus getJobStatus(String jobId) throws Exception {
        ResultActions response = callGetJobStatus(jobId);
        // then
        response.andDo(log())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", Matchers.notNullValue()));
        String responseAsString = response.andReturn().getResponse().getContentAsString();
        String status = MAPPER.readTree(responseAsString).get("jobStatus").asText();
        assertNotNull(status, "status should not be null");
        return JobStatus.valueOf(status);
    }

    protected Callable<JobStatus> jobProcessed(String jobId) {
        return () -> getJobStatus(jobId);
    }

    protected Callable<Boolean> isJobFinished(String jobId) {
        return () -> (getJobStatus(jobId).equals(JobStatus.FINISHED));
    }

    protected Callable<Boolean> isJobRunning(String jobId) {
        return () -> (getJobStatus(jobId).equals(JobStatus.RUNNING));
    }

    protected Callable<Boolean> isJobAborted(String jobId) {
        return () -> (getJobStatus(jobId).equals(JobStatus.ABORTED));
    }

    @NotNull
    protected ResultActions callGetJobStatus(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/status/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = getMockMvcObject().perform(requestBuilder);
        return response;
    }

    protected String callRunAPIAndVerify(
            String query, String fields, String sort, String format, boolean includeIsoform)
            throws Exception {

        ResultActions response = callPostJobStatus(query, fields, sort, format, includeIsoform);

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

    protected void getAndVerifyDetails(String jobId) throws Exception {
        // when
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.query", Matchers.notNullValue()))
                .andExpect(jsonPath("$.redirectURL", Matchers.endsWith(jobId)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @NotNull
    protected ResultActions callGetJobDetails(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/details/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = getMockMvcObject().perform(requestBuilder);
        return response;
    }

    protected abstract ResultActions callPostJobStatus(
            String query, String fields, String sort, String format, boolean includeIsoform)
            throws Exception;

    protected abstract void verifyIdsAndResultFiles(String jobId) throws IOException;

    protected abstract void verifyIdsFile(String jobId) throws IOException;

    protected abstract Stream<Arguments> getSupportedFormats();

    protected abstract String getDownloadAPIsBasePath();

    protected abstract String submitJobWithQuery();

    protected abstract String submitJobWithSortFields();

    protected abstract List<String> submitJobWithSortResultIds();

    protected abstract String getSubmitJobAllFormatQuery();

    protected abstract String getSubmitJobAllFormatQueryFields();

    protected abstract MediaType getUnsupportedFormat();

    protected abstract String getUnsupportedFormatErrorMsg();

    protected abstract String getSubmitJobUnsupportedFormatFailureFields();

    protected abstract String getRunQueryWhichReturnsEmptyResult();

    protected abstract String getQueryForJSONAndTSVRunJobWithFields();

    protected abstract String getQueryFieldsForJSONAndTSVRunJobWithFields();

    protected abstract String getRunJobHeaderWithFieldsTSV();

    protected abstract String getResultIdStringToMatch();

    protected abstract String submitJobWithoutFormatDefaultsToJsonGetField();
}
