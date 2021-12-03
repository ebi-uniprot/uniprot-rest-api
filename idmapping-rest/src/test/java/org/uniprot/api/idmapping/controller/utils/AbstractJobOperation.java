package org.uniprot.api.idmapping.controller.utils;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public abstract class AbstractJobOperation implements JobOperation {
    public static final int DEFAULT_IDS_COUNT = 20;// same as id.mapping.max.from.ids.count
    private IdMappingJobCacheService cacheService;

    public AbstractJobOperation(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
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

    public IdMappingJob createAndPutJobInCache() throws Exception {
        return createAndPutJobInCache(DEFAULT_IDS_COUNT);
    }

    public IdMappingJob createAndPutJobInCache(JobStatus jobStatus) throws Exception {
        return createAndPutJobInCache(DEFAULT_IDS_COUNT, jobStatus);
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

    private String generateHash(IdMappingJobRequest request) {
        return UUID.randomUUID().toString();
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
