package org.uniprot.api.async.download.controller;

import static java.util.function.Predicate.isEqual;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.common.RedisUtil.jobCreatedInRedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.AbstractIdMappingIT;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {AsyncDownloadRestApp.class})
@WebMvcTest(IdMappingDownloadController.class)
@ExtendWith(value = {SpringExtension.class})
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingDownloadControllerIT extends AbstractIdMappingIT {

    @Autowired protected MockMvc mockMvc;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String JOB_SUBMIT_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/run";

    protected static final String JOB_STATUS_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/status/{jobId}";

    protected static final String JOB_DETAILS_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/details/{jobId}";

    protected Callable<JobStatus> jobProcessed(String jobId) {
        return () -> getJobStatus(jobId);
    }

    protected JobStatus getJobStatus(String jobId) throws Exception {

        await().until(jobCreatedInRedis(downloadJobRepository, jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", notNullValue()));
        String responseAsString = response.andReturn().getResponse().getContentAsString();
        assertNotNull(responseAsString, "status response should not be null");
        String status = MAPPER.readTree(responseAsString).get("jobStatus").asText();
        assertNotNull(status, "status should not be null");
        JobStatus jobStatus = JobStatus.valueOf(status);
        long totalEntries = MAPPER.readTree(responseAsString).get("totalEntries").asLong();
        long processedEntries = MAPPER.readTree(responseAsString).get("processedEntries").asLong();
        if (JobStatus.FINISHED.equals(jobStatus)) {
            assertEquals(totalEntries, processedEntries);
        } else {
            assertTrue(processedEntries <= totalEntries);
        }
        return jobStatus;
    }

    @Test
    void downloadJobDetailsNotFound() throws Exception {
        // Do not save request in idmapping cache
        String jobId = "JOB_DETAILS_NOT_FOUND";

        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void downloadCanGetJobDetails() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS";
        String asynchJobId = "26us1hCFLb";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asynchJobId)));

        await().until(jobProcessed(asynchJobId), isEqual(JobStatus.FINISHED));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, asynchJobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                is("https://localhost/idmapping/download/results/" + asynchJobId)))
                .andExpect(jsonPath("$.fields", is("accession")))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)));
    }

    @Test
    void downloadCanGetJobDetailsWithError() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .status(JobStatus.ERROR)
                        .error("Error message")
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, asyncJobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code", is(PredefinedAPIStatus.SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(downloadJob.getError())));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJob() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJobWithAcceptAll() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.ALL)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJobWithoutAccept() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFailedJobAfterMaxRetry() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_RETRY";
        String asyncJobId = "3Q4ID5wQxK";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, mappedIds, unMappedIds);
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .retried(3)
                        .status(JobStatus.ERROR)
                        .build();
        downloadJobRepository.save(downloadJob);

        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        Assertions.assertTrue(resultFilePath.toFile().createNewFile());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message").doesNotExist());

        validateSuccessIdMappingResult(asyncJobId, resultFilePath, unMappedIds);
    }

    @Test
    void resubmit_withForceOnAlreadyFailedJobBeforeMaxRetry() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .retried(1)
                        .status(JobStatus.ERROR)
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("already being retried")));
    }

    @Test
    void downloadJobSubmittedNotFound() throws Exception {
        // Do not save request in idmapping cache

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", "JOB_NOT_FOUND")
                                .param("format", "json"));
        // then
        response.andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPARC_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(jobId, "UniParc", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=list]")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniParc fields parameter value: [invalid].")));
    }

    @Test
    void uniParcDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPARC_JOB_SUCCESS";
        String asyncJobId = "omGWzNTCvM";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");
        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "upi,organism"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());

        validateSuccessIdMappingResult(asyncJobId, resultFilePath, unMappedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniParcFormats")
    void uniParcDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));

        String jobId = "UNIPARC_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));

        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("UPI0000283A01"));
            assertTrue(text.contains("UPI0000283A02"));
        }
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIREF_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        cacheIdMappingJob(jobId, "UniRef90", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniRef100", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples]")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniRef100", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniRef fields parameter value: [invalid].")));
    }

    @Test
    void unirefDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIREF_JOB_SUCCESS";
        String asyncJobId = "9IwZyXSe7G";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        List<String> unMappedIds = List.of("UniRef90_P03001", "UniRef90_P03002");
        cacheIdMappingJob(jobId, "UniRef90", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "id,name,common_taxon"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P10001", "P10002")));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("id").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("UniRef90_P03901", "UniRef90_P03902")));

        assertTrue(
                to.stream()
                        .map(node -> node.findPath("members"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());

        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniRefFormats")
    void unirefDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef50_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef50_P03902"));

        String jobId = "UNIREF_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniRef50", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));

        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("UniRef50_P03901"));
            assertTrue(text.contains("UniRef50_P03902"));
        }
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPROTKB_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=flatfile, text/plain;format=gff, text/plain;format=list]")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniProtKB fields parameter value: [invalid].")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPROTKB_JOB_SUCCESS";
        String asyncJobId = "dMcurbvIQ8";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        List<String> unMappedIds = List.of("P12345", "P54321");
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession,gene_names,organism_name"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P00001", "P00001")));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("primaryAccession").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("P00001", "P00001")));

        assertTrue(
                to.stream()
                        .map(node -> node.findPath("features"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());

        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniProtKBFormats")
    void uniProtKBDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));

        String jobId = "UNIPROTKB_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));

        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("P00001"));
            assertTrue(text.contains("P00002"));
        }
    }

    private void validateSuccessIdMappingResult(
            String asyncJobId, Path resultFilePath, List<String> unMappedIds) throws IOException {
        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P10001", "P10002")));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("uniParcId").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("UPI0000283A01", "UPI0000283A02")));

        assertTrue(
                to.stream()
                        .map(node -> node.findPath("sequence"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());

        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }
}
