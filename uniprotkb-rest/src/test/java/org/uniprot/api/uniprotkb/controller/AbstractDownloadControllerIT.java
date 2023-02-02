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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.BasicSearchController.EXCEPTION_CODE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadControllerIT extends AbstractUniProtKBDownloadIT {

    @Autowired private MockMvc mockMvc;
    private static ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void runStarQuerySuccess() throws Exception {
        String query = "*:*";
        MediaType contentType = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, contentType);
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
    void runStarQueryMoreThanOnceShouldProcessOnlyOnceSuccess() throws Exception {
        String query = "*";
        MediaType contentType = MediaType.APPLICATION_JSON;
        String jobId = callRunAPIAndVerify(query, null, contentType);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId, contentType);
        Optional<DownloadJob> optJob1 = getDownloadJobRepository().findById(jobId);
        String newJobId = callRunAPIAndVerify(query, null, contentType);
        assertNotNull(newJobId);
        assertEquals(jobId, newJobId);
        Optional<DownloadJob> optJob2 = getDownloadJobRepository().findById(jobId);
        assertAll(() -> assertTrue(optJob1.isPresent()), () -> assertTrue(optJob2.isPresent()));
        assertEquals(optJob1.get(), optJob2.get());
        verifyIdsAndResultFiles(jobId, contentType);
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getSupportedContentTypes")
    void submitJobAllContentType(MediaType contentType) throws Exception {
        // when
        String query = "content:*";
        String fields = "accession,rhea";
        String jobId = callRunAPIAndVerify(query, fields, contentType);
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
        String jobId = callRunAPIAndVerify(query, fields, null);
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
        // then
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("contentType", contentType.toString());
        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", Matchers.contains("Invalid content type received, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'. Expected one of [text/plain;format=tsv, application/json, text/plain;format=flatfile, text/plain;format=list, application/xml, text/plain;format=fasta, text/plain;format=gff, application/rdf+xml].")));
    }

    @Test
    void submitJobWithBadQuery() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "random:field";
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("contentType", contentType.toString());

        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("'random' is not a valid search field")));
    }

    @Test
    void submitJobWithBadField() throws Exception {
        MediaType contentType = MediaType.APPLICATION_JSON;
        String query = "content:*";
        String fields = "something,else";
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("contentType", contentType.toString());

        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder("Invalid fields parameter value 'else'","Invalid fields parameter value 'something'")));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.ERROR.toString())))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0].code", is(EXCEPTION_CODE)))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
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
        response.andDo(print())
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

    protected String callRunAPIAndVerify(String query, String fields, MediaType contentType)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("contentType", Objects.isNull(contentType) ? null : contentType.toString());
        ResultActions response = this.mockMvc.perform(requestBuilder);

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

    protected abstract String getDownloadAPIsBasePath();

    protected void getAndVerifyDetails(String jobId, MediaType contentType) throws Exception {
        ResultActions response = callGetJobDetails(jobId);

        // then
        String expectedResult = jobId + "." + UniProtMediaType.getFileExtension(contentType);
        response.andDo(print())
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
