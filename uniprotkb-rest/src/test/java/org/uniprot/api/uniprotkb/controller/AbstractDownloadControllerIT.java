package org.uniprot.api.uniprotkb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.BasicSearchController.EXCEPTION_CODE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractUniProtKBDownloadIT {

    @Autowired private MockMvc mockMvc;
    private static ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void submitJobWithoutQueryFailure() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        ResultActions resultActions = callPostJobStatus(null, null, null, contentType);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.messages", contains("'query' is a required parameter")));
    }

    @Test
    void submitJobWithStarQuerySuccess() throws Exception {
        String query = "*:*";
        MediaType contentType = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, contentType);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, contentType);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(redirectedUrlPattern("**/uniprotkb/download/results/" + jobId + ".json"))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verifyIdsAndResultFiles(jobId, MediaType.APPLICATION_JSON);
    }

    @Test
    void submitJobStarQueryMoreThanOnceShouldProcessOnlyOnceSuccess() throws Exception {
        String query = "*";
        MediaType contentType = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null,  contentType);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, contentType);
        Optional<DownloadJob> optJob1 = getDownloadJobRepository().findById(jobId);
        String newJobId = callRunAPIAndVerify(query, null, null, contentType);
        assertNotNull(newJobId);
        assertEquals(jobId, newJobId);
        Optional<DownloadJob> optJob2 = getDownloadJobRepository().findById(jobId);
        assertAll(() -> assertTrue(optJob1.isPresent()), () -> assertTrue(optJob2.isPresent()));
        assertEquals(optJob1.get(), optJob2.get());
        verifyIdsAndResultFiles(jobId, contentType);
    }

    @Test
    void submitJobWithSort() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String sort = "accession desc";
        String jobId = callRunAPIAndVerify(query, null, sort, contentType);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        // then
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        MatcherAssert.assertThat(ids, contains("P00010", "P00009", "P00008", "P00007", "P00006", "P00005", "P00004", "P00003", "P00002", "P00001"));
        // verify result file
        String fileExt = "." + UniProtMediaType.getFileExtension(contentType);
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId + fileExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        String resultsJson = Files.readString(resultFilePath);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        MatcherAssert.assertThat(primaryAccessions, contains("P00010", "P00009", "P00008", "P00007", "P00006", "P00005", "P00004", "P00003", "P00002", "P00001"));
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getSupportedContentTypes")
    void submitJobAllContentType(MediaType contentType) throws Exception {
        // when
        String query = "content:*";
        String fields = "accession,rhea";
        String jobId = callRunAPIAndVerify(query, fields, null, contentType);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().atMost(30, SECONDS).until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, contentType);
    }

    @Test
    void submitJobWithoutContentTypeDefaultsToJson() throws Exception {
        // when
        String query = "content:*";
        String fields = "accession";
        String jobId = callRunAPIAndVerify(query, fields, null, null);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, MediaType.APPLICATION_JSON);
    }
    @Test
    void submitJobUnsupportedContentTypeFailure() throws Exception {
        // when
        MediaType contentType = UniProtMediaType.valueOf(XLS_MEDIA_TYPE_VALUE);
        String query = "content:*";
        String fields = "accession,rhea";
        ResultActions response = callPostJobStatus(query, fields, null, contentType);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", Matchers.contains("Invalid content type received, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'. Expected one of [text/plain;format=tsv, application/json, text/plain;format=flatfile, text/plain;format=list, application/xml, text/plain;format=fasta, text/plain;format=gff, application/rdf+xml].")));
    }

    @Test
    void submitJobWithBadQuery() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "random:field";
        ResultActions response = callPostJobStatus(query, null, null, contentType);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("'random' is not a valid search field")));
    }

    @Test
    void submitJobWithBadField() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String fields = "something,else";

        ResultActions response = callPostJobStatus(query, fields, null, contentType);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder("Invalid fields parameter value 'else'","Invalid fields parameter value 'something'")));
    }

    @Test
    void submitJobWithBadSort() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String sort = "something desc";

        ResultActions response = callPostJobStatus(query, null, sort, contentType);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder("Invalid sort field 'something'")));
    }

    @Test
    void getStatusReturnsError() throws Exception {
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
                .andExpect(jsonPath("$.errors[0].code", is(EXCEPTION_CODE)))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    void getStatusReturnsNew() throws Exception {
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
    void getStatusForUnknownJobId() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ResultActions response = callGetJobStatus(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", contains("Resource not found")));
    }

    @Test
    void getDetailsForRunningJob() throws Exception {
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
    void getDetailsForUnknownJobId() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ResultActions response = callGetJobDetails(jobId);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", contains("Resource not found")));
    }

    @Test
    void getDetailsWithErroredJob() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String errMsg = "this is a friendly error message";
        String query = "sample:query";
        String fields = "sample,fields";
        String sort = "sample sort";
        DownloadJob job = builder.id(jobId).query(query).sort(sort).fields(fields).status(JobStatus.ERROR)
                .error(errMsg).contentType(APPLICATION_JSON_VALUE).build();
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
                .andExpect(jsonPath("$.contentType", is(APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(EXCEPTION_CODE)))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    void runQueryWhichReturnsEmptyResult() throws Exception {
        String query = "content:khansamatola";
        MediaType contentType = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, contentType);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, contentType);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(redirectedUrlPattern("**/uniprotkb/download/results/" + jobId + ".json"))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());

        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(ids.isEmpty());
        // verify result file
        String fileExt = "." + UniProtMediaType.getFileExtension(contentType);
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId + fileExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        String resultsJson = Files.readString(resultFilePath);
        List<String> results = JsonPath.read(resultsJson, "$.results");
        Assertions.assertAll(() -> assertNotNull(results), () -> assertTrue(results.isEmpty()));
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

    private Callable<JobStatus> jobProcessed(String jobId) {
        return () -> getJobStatus(jobId);
    }

    @NotNull
    private ResultActions callGetJobStatus(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/status/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    protected String callRunAPIAndVerify(String query, String fields, String sort, MediaType contentType)
            throws Exception {

        ResultActions response = callPostJobStatus(query, fields, sort, contentType);

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

    private ResultActions callPostJobStatus(String query, String fields, String sort, MediaType contentType) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("sort", sort)
                        .param("contentType", Objects.isNull(contentType) ? null : contentType.toString());
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    protected abstract String getDownloadAPIsBasePath();

    protected void getAndVerifyDetails(String jobId, MediaType contentType) throws Exception {
        // when
        ResultActions response = callGetJobDetails(jobId);
        // then
        String expectedResult = jobId + "." + UniProtMediaType.getFileExtension(contentType);
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.query", Matchers.notNullValue()))
                .andExpect(jsonPath("$.redirectURL", Matchers.endsWith(expectedResult)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @NotNull
    private ResultActions callGetJobDetails(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/details/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    protected abstract void verifyIdsAndResultFiles(String jobId, MediaType contentType)
            throws IOException;

    private Stream<Arguments> getSupportedContentTypes() {
        return List.of(
                        UniProtMediaType.valueOf(TSV_MEDIA_TYPE_VALUE),
                        UniProtMediaType.valueOf(FF_MEDIA_TYPE_VALUE),
                        UniProtMediaType.valueOf(LIST_MEDIA_TYPE_VALUE),
                        UniProtMediaType.valueOf(APPLICATION_XML_VALUE),
                        UniProtMediaType.valueOf(APPLICATION_JSON_VALUE),
                        UniProtMediaType.valueOf(FASTA_MEDIA_TYPE_VALUE),
                        UniProtMediaType.valueOf(GFF_MEDIA_TYPE_VALUE),
                        UniProtMediaType.valueOf(RDF_MEDIA_TYPE_VALUE))
                .stream()
                .map(Arguments::of);
    }
}
