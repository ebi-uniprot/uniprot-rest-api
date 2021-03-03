package org.uniprot.api.idmapping.controller;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public abstract class AbstractJobOperation implements JobOperation {
    private IdMappingJobCacheService cacheService;
    private HashGenerator hashGenerator;

    public AbstractJobOperation(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
        this.hashGenerator = new HashGenerator();
    }

    public IdMappingJob createAndPutJobInCache(String from, String to, String fromIds)
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
        return createAndPutJobInCache(from, to, mappedIds, JobStatus.FINISHED);
    }

    protected IdMappingJob createAndPutJobInCache(
            String from, String to, Map<String, String> mappedIds, JobStatus jobStatus)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        String fromIds = String.join(",", mappedIds.keySet());
        IdMappingJobRequest idMappingRequest = createRequest(from, to, fromIds);
        String jobId = generateHash(idMappingRequest);
        IdMappingResult idMappingResult = createIdMappingResult(idMappingRequest, mappedIds);
        IdMappingJob job = createJob(jobId, idMappingRequest, idMappingResult, jobStatus);
        if (!this.cacheService.exists(jobId)) {
            this.cacheService.put(jobId, job); // put the finished job in cache
        }
        return job;
    }

    protected IdMappingJobRequest createRequest(String from, String to, String fromIds) {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setIds(fromIds);
        return request;
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
