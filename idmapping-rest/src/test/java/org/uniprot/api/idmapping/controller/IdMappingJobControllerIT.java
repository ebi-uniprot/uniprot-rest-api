package org.uniprot.api.idmapping.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.IdMappingWarning;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {IdMappingREST.class})
@WebMvcTest(IdMappingJobController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingJobControllerIT {
    @Value("${id.mapping.max.from.ids.count}")
    private Integer maxCountCSV;

    @Value("${id.mapping.max.to.ids.enrich.count}")
    private Integer maxAllowedIdsToEnrich;

    private static final String JOB_SUBMIT_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/run";
    private static final String JOB_STATUS_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/status/{jobId}";
    private static final String JOB_DETAILS_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/details/{jobId}";

    @Autowired private MockMvc mockMvc;
    @MockBean private IdMappingJobCacheService cacheService;

    @Test
    void jobSubmittedSuccessfully() throws Exception {
        // when
        IdMappingJobRequest basicRequest = new IdMappingJobRequest();
        basicRequest.setFrom("UniProtKB_AC-ID");
        basicRequest.setTo("EMBL-GenBank-DDBJ");
        basicRequest.setIds("Q1,Q2");

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", basicRequest.getFrom())
                                .param("to", basicRequest.getTo())
                                .param("ids", basicRequest.getIds()));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", Matchers.notNullValue()));

        String jobId = extractJobId(response);

        ArgumentCaptor<IdMappingJob> jobCaptor = ArgumentCaptor.forClass(IdMappingJob.class);
        verify(cacheService).put(eq(jobId), jobCaptor.capture());
        assertThat(jobCaptor.getValue().getIdMappingRequest(), is(basicRequest));
    }

    @Test
    void finishedJobShowsFinishedStatusWithCorrectRedirect() throws Exception {
        String jobId = "ID";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL-GenBank-DDBJ");

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.FINISHED)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(redirectedUrlPattern("**/idmapping/results/" + jobId))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void nonExistentJobStatusIsNotFound() throws Exception {
        String jobId = "JOB_ID_THAT_DOES_NOT_EXIST";

        when(cacheService.getJobAsResource(jobId)).thenThrow(ResourceNotFoundException.class);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    @Test
    void newJobHasRunningStatus() throws Exception {
        String jobId = "ID";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL-GenBank-DDBJ");

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.NEW)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.NEW.toString())))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void runningJobHasRunningStatus() throws Exception {
        String jobId = "ID";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL-GenBank-DDBJ");

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.RUNNING)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.RUNNING.toString())))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void jobThatErroredHasErrorStatus() throws Exception {
        String jobId = "ID";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL-GenBank-DDBJ");

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.ERROR)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.ERROR.toString())))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    private String extractJobId(ResultActions response)
            throws UnsupportedEncodingException, JsonProcessingException {
        String contentAsString = response.andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(contentAsString).get("jobId").asText();
    }

    @Test
    void submittingJobWithInvalidFromCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC123")
                                .param("to", "UniProtKB")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                containsInAnyOrder(
                                        "The parameter 'from' has an invalid value 'ACC123'.",
                                        "The combination of 'from=ACC123' and 'to=UniProtKB' parameters is invalid")));
    }

    @Test
    void submittingJobWithInvalidToCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "UniProtKB_AC-ID")
                                .param("to", "ACC123")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                contains(
                                        "The parameter 'to' has an invalid value 'ACC123'.",
                                        "The combination of 'from=UniProtKB_AC-ID' and 'to=ACC123' parameters is invalid")));
    }

    @Test
    void submittingJobWithInvalidFromToPairCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "EMBL-GenBank-DDBJ")
                                .param("to", "UniParc")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                contains(
                                        "The combination of 'from=EMBL-GenBank-DDBJ' and 'to=UniParc' parameters is invalid")));
    }

    @Test
    void submittingJobWithInvalidFromToWithTaxIdCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "EMBL-GenBank-DDBJ")
                                .param("to", "UniParc")
                                .param("taxId", "taxId")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                containsInAnyOrder(
                                        "The combination of 'from=EMBL-GenBank-DDBJ' and 'to=UniParc' parameters is invalid",
                                        "Invalid parameter 'taxId'")));
    }

    @Test
    void submittingJobWithValidFromToWithInvalidTaxIdCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "EMBL-GenBank-DDBJ")
                                .param("to", "UniProtKB")
                                .param("taxId", "taxId")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[*]", contains("Invalid parameter 'taxId'")));
    }

    @Test
    void jobSubmittedMoreThanAllowedIdsFailed() throws Exception {
        // when
        // allowedSize+1 ids
        String ids =
                IntStream.rangeClosed(0, this.maxCountCSV)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));
        IdMappingJobRequest basicRequest = new IdMappingJobRequest();
        basicRequest.setFrom("UniProtKB_AC-ID");
        basicRequest.setTo("EMBL-GenBank-DDBJ");
        basicRequest.setIds(ids);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", basicRequest.getFrom())
                                .param("to", basicRequest.getTo())
                                .param("ids", basicRequest.getIds()));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "'"
                                                + this.maxCountCSV
                                                + "' is the maximum count limit of comma separated items for 'ids' param. You have passed '"
                                                + (this.maxCountCSV + 1)
                                                + "' items.")));
    }

    @Test
    void validJobDetailsReturnDetails() throws Exception {
        String jobId = "ID";
        String ids = "Q1,Q2";
        String taxId = "9606";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(IdMappingFieldConfig.ACC_ID_STR);
        request.setTo(IdMappingFieldConfig.GENE_NAME_STR);
        request.setIds(ids);
        request.setTaxId(taxId);

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.NEW)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from", is(IdMappingFieldConfig.ACC_ID_STR)))
                .andExpect(jsonPath("$.to", is(IdMappingFieldConfig.GENE_NAME_STR)))
                .andExpect(jsonPath("$.ids", is(ids)))
                .andExpect(jsonPath("$.taxId", is(taxId)))
                .andExpect(jsonPath("$.redirectURL").doesNotExist())
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void validJobDetailsStatusFinishedReturnDetailsWithRedirectURL() throws Exception {
        String jobId = "ID";
        String ids = "Q1,Q2";
        String taxId = "9606";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(IdMappingFieldConfig.ACC_ID_STR);
        request.setTo(IdMappingFieldConfig.UNIPARC_STR);
        request.setIds(ids);
        request.setTaxId(taxId);

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.FINISHED)
                        .jobId(jobId)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from", is(IdMappingFieldConfig.ACC_ID_STR)))
                .andExpect(jsonPath("$.to", is(IdMappingFieldConfig.UNIPARC_STR)))
                .andExpect(jsonPath("$.ids", is(ids)))
                .andExpect(jsonPath("$.taxId", is(taxId)))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                matchesPattern(
                                        "https://localhost/idmapping/uniparc/results/" + jobId)))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void nonExistentJobDetailsIsNotFound() throws Exception {
        String jobId = "JOB_ID_THAT_DOES_NOT_EXIST";

        when(cacheService.getJobAsResource(jobId)).thenThrow(ResourceNotFoundException.class);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    /**
     * If the mapped ids are more than id.mapping.max.to.ids.enrich.count
     * we return the plain from and to result without any uniprot data in the status/{jobId} response.
     * So to do that we just return the plain results url redirect without any db in the path even though the mapped to id
     * is uniprotkb, uniparc or uniref ids
     * plain result url : https://localhost/idmapping/results/{jobId}
     *
     */

    @Test
    void testGetJobStatusWithCorrectRedirect() throws Exception {
        String jobId = "ID";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom("UniProtKB_AC-ID");
        request.setTo("UniProtKB");
        request.setIds("Q00001,Q00002");

        // map more than allowed enrich ids
        IdMappingResult idMappingResult = IdMappingResult.builder()
                .mappedIds(getMappedIds(this.maxAllowedIdsToEnrich))
                .warning(IdMappingWarning.ENRICHMENT)
                .build();

        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.FINISHED)
                        .jobId(jobId)
                        .idMappingResult(idMappingResult)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(redirectedUrlPattern("**/idmapping/results/" + jobId))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.warnings.length()", is(1)))
                .andExpect(jsonPath("$.warnings[0].message", is(IdMappingWarning.ENRICHMENT.getMessage())))
                .andExpect(jsonPath("$.warnings[0].code", is(IdMappingWarning.ENRICHMENT.getCode())));
    }

    /**
     * If the mapped ids are more than id.mapping.max.to.ids.enrich.count
     * we return the plain from and to result without any uniprot data.
     * So to do that we just return the plain results url redirect without any db in the path even though the mapped to id
     * is uniprotkb, uniparc or uniref ids
     * plain result url : https://localhost/idmapping/results/{jobId}
     *
     */
    @Test
    void testGetJobDetailsWithCorrectRedirectUrl() throws Exception {
        String jobId = "ID";
        String ids = "Q1,Q2";
        String taxId = "9606";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(IdMappingFieldConfig.ACC_ID_STR);
        request.setTo(IdMappingFieldConfig.UNIPARC_STR);
        request.setIds(ids);
        request.setTaxId(taxId);
        IdMappingResult idMappingResult = IdMappingResult.builder()
                .mappedIds(getMappedIds(this.maxAllowedIdsToEnrich))
                .warning(IdMappingWarning.ENRICHMENT)
                .build();
        IdMappingJob job =
                IdMappingJob.builder()
                        .idMappingRequest(request)
                        .jobStatus(JobStatus.FINISHED)
                        .jobId(jobId)
                        .idMappingResult(idMappingResult)
                        .build();
        when(cacheService.getJobAsResource(jobId)).thenReturn(job);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from", is(IdMappingFieldConfig.ACC_ID_STR)))
                .andExpect(jsonPath("$.to", is(IdMappingFieldConfig.UNIPARC_STR)))
                .andExpect(jsonPath("$.ids", is(ids)))
                .andExpect(jsonPath("$.taxId", is(taxId)))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                matchesPattern(
                                        "https://localhost/idmapping/results/" + jobId)))
                .andExpect(jsonPath("$.warnings.length()", is(1)))
                .andExpect(jsonPath("$.warnings[0].message", is(IdMappingWarning.ENRICHMENT.getMessage())))
                .andExpect(jsonPath("$.warnings[0].code", is(IdMappingWarning.ENRICHMENT.getCode())));
    }

    private Collection<IdMappingStringPair> getMappedIds(Integer idPairCountPlusOne) {
        return IntStream.rangeClosed(0, idPairCountPlusOne)
                .mapToObj(i -> new IdMappingStringPair("from", "to"))
                .collect(Collectors.toList());
    }
}
