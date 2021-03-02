package org.uniprot.api.idmapping.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingResultsControllerIT extends AbstractStreamControllerIT {

    @Autowired protected IdMappingJobCacheService idMappingJobCacheService;

    protected abstract MockMvc getMockMvc();

    protected abstract String getIdMappingResultPath();

    protected abstract IdMappingJob createAndPutJobInCache() throws Exception;

    @Test
    void testIdMappingResultOnePage() throws Exception {
        // when
        Integer defaultPageSize = 5;
        IdMappingJob job = createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(defaultPageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(ids[0], ids[1], ids[2], ids[3], ids[4])));
    }

    @Test
    void testIdMappingResultWithSize() throws Exception {
        // when
        Integer size = 10;
        IdMappingJob job = createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[0], ids[1], ids[2], ids[3], ids[4], ids[5], ids[6],
                                        ids[7], ids[8], ids[9])));
    }

    @Test
    void testIdMappingResultWithSizeAndPagination() throws Exception {
        // when
        Integer size = 10;
        IdMappingJob job = createAndPutJobInCache();
        String[] ids = job.getIdMappingRequest().getIds().split(",");

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[0], ids[1], ids[2], ids[3], ids[4], ids[5], ids[6],
                                        ids[7], ids[8], ids[9])));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        // when 2nd page
        response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size))
                                        .param("cursor", cursor));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        ids[10], ids[11], ids[12], ids[13], ids[14], ids[15],
                                        ids[16], ids[17], ids[18], ids[19])));
    }

    @Test
    void testIdMappingResultMappingWithZeroSize() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", "0"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testIdMappingResultWithNegativeSize() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", "-1"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @Test
    void testIdMappingResultWithMoreThan500Size() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", "600"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    protected final HashGenerator hashGenerator = new HashGenerator();

    protected IdMappingJobRequest createRequest(String from, String to, String fromIds) {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setIds(fromIds);
        return request;
    }

    protected IdMappingJob createAndPutJobInCache(String from, String to, String fromIds)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        Map<String, String> mappedIds =
                Arrays.stream(fromIds.split(","))
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        Function.identity(),
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        return createAndPutJobInCache(from, to, mappedIds);
    }

    protected IdMappingJob createAndPutJobInCache(
            String from, String to, Map<String, String> mappedIds)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        String fromIds = String.join(",", mappedIds.keySet());
        IdMappingJobRequest idMappingRequest = createRequest(from, to, fromIds);
        String jobId = generateHash(idMappingRequest);
        IdMappingResult idMappingResult = createIdMappingResult(idMappingRequest, mappedIds);
        IdMappingJob job = createJob(jobId, idMappingRequest, idMappingResult, JobStatus.FINISHED);
        if (!this.idMappingJobCacheService.exists(jobId)) {
            this.idMappingJobCacheService.put(jobId, job); // put the finished job in cache
        }
        return job;
    }

    private String generateHash(IdMappingJobRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        return this.hashGenerator.generateHash(request);
    }

    private IdMappingResult createIdMappingResult(
            IdMappingJobRequest request, Map<String, String> mappedIds) {
        List<IdMappingStringPair> ids =
                mappedIds.entrySet().stream()
                        .map(entry -> new IdMappingStringPair(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
        return IdMappingResult.builder().mappedIds(ids).build();
    }

    private IdMappingJob createJob(
            String jobId,
            IdMappingJobRequest request,
            IdMappingResult result,
            JobStatus jobStatus) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(jobStatus);
        builder.idMappingRequest(request).idMappingResult(result);
        return builder.build();
    }
}
