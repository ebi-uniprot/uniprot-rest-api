package org.uniprot.api.uniprotkb.controller;

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
import static org.uniprot.api.uniprotkb.controller.TestUtils.uncompressFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

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
import org.uniprot.api.rest.output.context.FileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractUniProtKBDownloadIT {

    @Autowired private MockMvc mockMvc;
    private static ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void submitJobWithoutQueryFailure() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        ResultActions resultActions = callPostJobStatus(null, null, null, format.toString());
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
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString());
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        ResultActions resultActions = callGetJobStatus(jobId);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", equalTo(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verifyIdsAndResultFiles(jobId, MediaType.APPLICATION_JSON);
    }

    @Test
    void submitJobStarQueryMoreThanOnceShouldProcessOnlyOnceSuccess() throws Exception {
        String query = "*";
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString());
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().atMost(20, SECONDS).until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        Optional<DownloadJob> optJob1 = getDownloadJobRepository().findById(jobId);
        String newJobId = callRunAPIAndVerify(query, null, null, format.toString());
        assertNotNull(newJobId);
        assertEquals(jobId, newJobId);
        Optional<DownloadJob> optJob2 = getDownloadJobRepository().findById(jobId);
        assertAll(() -> assertTrue(optJob1.isPresent()), () -> assertTrue(optJob2.isPresent()));
        assertEquals(optJob1.get(), optJob2.get());
        verifyIdsAndResultFiles(jobId, format);
    }

    @Test
    void submitJobWithSort() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String sort = "accession desc";
        String jobId = callRunAPIAndVerify(query, null, sort, format.toString());
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        // then
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        MatcherAssert.assertThat(
                ids,
                contains(
                        "P00010", "P00009", "P00008", "P00007", "P00006", "P00005", "P00004",
                        "P00003", "P00002", "P00001"));
        // verify result file

        Path resultFilePath =
                Path.of(this.resultFolder + "/" + jobId + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        MatcherAssert.assertThat(
                primaryAccessions,
                contains(
                        "P00010", "P00009", "P00008", "P00007", "P00006", "P00005", "P00004",
                        "P00003", "P00002", "P00001"));
    }

    @ParameterizedTest(name = "[{index}] format {0}")
    @MethodSource("getSupportedFormats")
    void submitJobAllFormat(String format) throws Exception {
        // when

        String query = "content:*";
        String fields = "accession,rhea";
        String jobId = callRunAPIAndVerify(query, fields, null, format);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().atMost(30, SECONDS).until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
    }

    @Test
    void submitJobWithoutFormatDefaultsToJson() throws Exception {
        // when
        String query = "content:*";
        String fields = "accession";
        String jobId = callRunAPIAndVerify(query, fields, null, null);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
    }

    @Test
    void submitJobUnsupportedFormatFailure() throws Exception {
        // when
        MediaType format = UniProtMediaType.valueOf(XLS_MEDIA_TYPE_VALUE);
        String query = "content:*";
        String fields = "accession,rhea";
        ResultActions response = callPostJobStatus(query, fields, null, format.toString());

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                Matchers.contains(
                                        "Invalid format received, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'. Expected one of [text/plain;format=tsv, application/json, text/plain;format=flatfile, text/plain;format=list, application/xml, text/plain;format=fasta, text/plain;format=gff, application/rdf+xml].")));
    }

    @Test
    void submitJobWithBadQuery() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "random:field";
        ResultActions response = callPostJobStatus(query, null, null, format.toString());
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath("$.messages.*", contains("'random' is not a valid search field")));
    }

    @Test
    void submitJobWithBadField() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String fields = "something,else";

        ResultActions response = callPostJobStatus(query, fields, null, format.toString());

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
    void submitJobWithBadSort() throws Exception {
        MediaType format = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String sort = "something desc";

        ResultActions response = callPostJobStatus(query, null, sort, format.toString());
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
                .andExpect(jsonPath("$.errors[0].code", is(EXCEPTION_CODE)))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    void runQueryWhichReturnsEmptyResult() throws Exception {
        String query = "content:khansamatola";
        MediaType format = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString());
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
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
    void runJobWithFieldsJsonAndVerify() throws Exception {
        // when
        String query = "content:FGFR";
        String fields = "accession,id,gene_names";
        String jobId = callRunAPIAndVerify(query, fields, null, "json");
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertEquals(10, primaryAccessions.size());
        List<String> uniProtkbIds = JsonPath.read(resultsJson, "$.results.*.uniProtkbId");
        Assertions.assertEquals(10, uniProtkbIds.size());
        List<String> genes = JsonPath.read(resultsJson, "$.results.*.genes");
        Assertions.assertEquals(10, genes.size());
        List<String> comments = JsonPath.read(resultsJson, "$.results.*.comments");
        Assertions.assertEquals(0, comments.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organism");
        Assertions.assertEquals(0, organisms.size());
    }

    @Test
    void runJobWithFieldsTSVAndVerify() throws Exception {
        // when
        String query = "content:FGFR";
        String fields = "accession,id,gene_names";
        String jobId = callRunAPIAndVerify(query, fields, null, "tsv");
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
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
        Assertions.assertEquals(11, rows.length);
        Assertions.assertEquals("Entry\tEntry Name\tGene Names", rows[0]);
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

    protected String callRunAPIAndVerify(String query, String fields, String sort, String format)
            throws Exception {

        ResultActions response = callPostJobStatus(query, fields, sort, format);

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

    private ResultActions callPostJobStatus(String query, String fields, String sort, String format)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("sort", sort)
                        .param("format", Objects.isNull(format) ? null : format);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    protected abstract String getDownloadAPIsBasePath();

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
    private ResultActions callGetJobDetails(String jobId) throws Exception {
        String jobStatusUrl = getDownloadAPIsBasePath() + "/details/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobStatusUrl, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    protected abstract void verifyIdsAndResultFiles(String jobId, MediaType format)
            throws IOException;

    private Stream<Arguments> getSupportedFormats() {
        return List.of(
                        "xml",
                        "json",
                        TSV_MEDIA_TYPE_VALUE,
                        FF_MEDIA_TYPE_VALUE,
                        LIST_MEDIA_TYPE_VALUE,
                        APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE,
                        FASTA_MEDIA_TYPE_VALUE,
                        GFF_MEDIA_TYPE_VALUE,
                        RDF_MEDIA_TYPE_VALUE)
                .stream()
                .map(Arguments::of);
    }
}
