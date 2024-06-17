package org.uniprot.api.async.download.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.controller.ControllerITUtils.NO_CACHE_VALUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.async.download.common.AbstractDownloadIT;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractDownloadIT {
    public static ObjectMapper MAPPER = new ObjectMapper();

    protected abstract MockMvc getMockMvcObject();

    @AfterEach
    void afterEach() throws IOException{
        FileUtils.cleanDirectory(new File(getTestAsyncConfig().getIdsFolder()));
        FileUtils.cleanDirectory(new File(getTestAsyncConfig().getResultFolder()));
        getDownloadJobRepository().deleteAll();
    }

    @Test
    protected void submitJobWithoutQueryFailure() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        ResultActions resultActions = callPostJobStatus(null, null, null, format.toString(), false, false);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_CACHE_VALUE))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.VARY,
                                        ACCEPT,
                                        ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
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
                .andExpect(jsonPath("$.totalEntries", equalTo(12)))
                .andExpect(jsonPath("$.processedEntries", equalTo(12)));
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
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        List<String> returnIds = submitJobWithSortResultIds();
        Assertions.assertNotNull(ids);
        assertTrue(ids.containsAll(returnIds));
        // verify result file
        Path resultFilePath =
                Path.of(
                        getTestAsyncConfig().getResultFolder()
                                + "/"
                                + jobId
                                + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
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
        String jobId = callRunAPIAndVerify(query, null, null, format, false);
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
        String jobId = callRunAPIAndVerify(query, null, null, null, false);
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
        ResultActions response = callPostJobStatus(query, null, null, format.toString(), false, false);

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
        ResultActions response = callPostJobStatus(query, null, null, format.toString(), false, false);
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

        ResultActions response = callPostJobStatus(query, fields, null, format.toString(), false, false);

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

        ResultActions response = callPostJobStatus(query, null, sort, format.toString(), false, false);
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
    protected void submitJobAlreadyFinished() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "alreadyFinishedQuery";
        String jobId = "950fcce1c0ffcc609ffe3c80f4cfc419b646e65d";

        DownloadJob job =
                getDownloadJob(
                        jobId, null, query, null, null, JobStatus.FINISHED, format.toString(), 0);
        getDownloadJobRepository().save(job);

        ResultActions response = callPostJobStatus(query, null, null, format.toString(), false, false);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(
                        jsonPath(
                                "$.message",
                                is("Job with id " + jobId + " has already been submitted")));
    }

    @Test
    protected void resubmit_withForceOnFailedJobAfterMaxRetryWillRunAgain() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "*:*";
        String jobId = "03fb6db0be39c4646f28bf6f83bcd600d79cab13";

        //Create Error Job in Redis
        DownloadJob job =
                getDownloadJob(
                        jobId,
                        "Error Message Value",
                        query,
                        null,
                        null,
                        JobStatus.ERROR,
                        format.toString(),
                        3);
        getDownloadJobRepository().save(job);
        await().until(() -> getDownloadJobRepository().existsById(jobId));

        //Create Files (Reproducing and Incomplete result file still in the file system)
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + jobId);
        Assertions.assertTrue(idsFilePath.toFile().createNewFile());
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId + ".gz");
        Assertions.assertTrue(resultFilePath.toFile().createNewFile());

        ResultActions response = callPostJobStatus(query, null, null, format.toString(), false, true);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

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
                .andExpect(jsonPath("$.totalEntries", equalTo(12)))
                .andExpect(jsonPath("$.processedEntries", equalTo(12)));
        verifyIdsAndResultFiles(jobId);
    }

    @Test
    protected void getStatusReturnsError() throws Exception {
        String jobId = UUID.randomUUID().toString();
        String errMsg = "this is a friendly error message";
        DownloadJob job = getDownloadJob(jobId, errMsg, null, null, null, JobStatus.ERROR, null, 0);
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
        DownloadJob job = getDownloadJob(jobId, null, null, null, null, JobStatus.NEW, null, 0);
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.NEW.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(0)))
                .andExpect(jsonPath("$.processedEntries", equalTo(0)));
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
        String query = "my:query";
        DownloadJob job =
                getDownloadJob(jobId, null, query, null, null, JobStatus.RUNNING, null, 0);
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
        String errMsg = "this is a friendly error message";
        String query = "sample:query";
        String fields = "sample,fields";
        String sort = "sample sort";
        DownloadJob job =
                getDownloadJob(
                        jobId,
                        errMsg,
                        query,
                        sort,
                        fields,
                        JobStatus.ERROR,
                        APPLICATION_JSON_VALUE,
                        0);
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
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.totalEntries", equalTo(0)))
                .andExpect(jsonPath("$.processedEntries", equalTo(0)));

        // verify the ids file
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(ids.isEmpty());
        // verify result file
        Path resultFilePath =
                Path.of(
                        getTestAsyncConfig().getResultFolder()
                                + "/"
                                + jobId
                                + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
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
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String tsvString = Files.readString(unzippedFile);
        String[] rows = tsvString.split("\n");
        Assertions.assertEquals(13, rows.length);
        Assertions.assertEquals(getRunJobHeaderWithFieldsTSV(), rows[0]);
    }

    /**
     * Tests that class level validation is called after field level validation. It also tests that
     * class level validation (see annotation on {@link
     * org.uniprot.controller.request.UniProtKBDownloadRequest} ) is not called if the fields
     * validation is failed.
     *
     * @throws Exception
     */
    @Test
    void submitJobWithInvalidFields() throws Exception {
        String query = "*:*";
        String fields = "randomfield1,randomfield2";
        String format = "fasta";
        ResultActions resultActions = callPostJobStatus(query, fields, null, format, false, false);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url").exists())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'randomfield1'",
                                        "Invalid fields parameter value 'randomfield2'")))
                .andExpect(
                        jsonPath(
                                "$.messages",
                                not(
                                        contains(
                                                "'fields' are not supported for 'format' "
                                                        + format))));
    }

    @ParameterizedTest
    @EnumSource(
            value = JobStatus.class,
            names = {"PROCESSING", "UNFINISHED"})
    void statusInProcessingOrUnFinishedReturnsRunning(JobStatus status) throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob job = getDownloadJob(jobId, null, null, null, null, status, null, 0);
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.RUNNING.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
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
        return getMockMvcObject().perform(requestBuilder);
    }

    protected String callRunAPIAndVerify(
            String query, String fields, String sort, String format, boolean includeIsoform)
            throws Exception {

        ResultActions response = callPostJobStatus(query, fields, sort, format, includeIsoform, false);

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
            String query, String fields, String sort, String format, boolean includeIsoform, boolean force)
            throws Exception;

    protected abstract void verifyIdsAndResultFiles(String jobId) throws IOException;

    protected abstract void verifyIdsFile(String jobId) throws IOException;

    protected abstract Stream<Arguments> getSupportedFormats();

    protected abstract String getDownloadAPIsBasePath();

    protected abstract String submitJobWithQuery();

    protected abstract String submitJobWithSortFields();

    protected abstract List<String> submitJobWithSortResultIds();

    protected abstract String getSubmitJobAllFormatQuery();

    protected abstract MediaType getUnsupportedFormat();

    protected abstract String getUnsupportedFormatErrorMsg();

    protected abstract String getRunQueryWhichReturnsEmptyResult();

    protected abstract String getQueryForJSONAndTSVRunJobWithFields();

    protected abstract String getQueryFieldsForJSONAndTSVRunJobWithFields();

    protected abstract String getRunJobHeaderWithFieldsTSV();

    protected abstract String getResultIdStringToMatch();

    protected abstract DownloadJob getDownloadJob(
            String jobId,
            String errMsg,
            String query,
            String sort,
            String fields,
            JobStatus jobStatus,
            String format,
            int retried);
}
