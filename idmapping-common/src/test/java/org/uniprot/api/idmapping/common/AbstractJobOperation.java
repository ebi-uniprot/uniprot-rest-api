package org.uniprot.api.idmapping.common;

import static org.uniprot.api.idmapping.common.service.PIRResponseConverter.*;
import static org.uniprot.api.idmapping.common.service.impl.PIRServiceImpl.UNIPROTKB_ACCESSION_REGEX;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public abstract class AbstractJobOperation implements JobOperation {
    public static final int DEFAULT_IDS_COUNT = 20; // same as mapping.max.from.ids.count
    private final IdMappingJobCacheService cacheService;

    public AbstractJobOperation(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public IdMappingJobCacheService getIdMappingJobCacheService() {
        return this.cacheService;
    }

    @Override
    public IdMappingJob createAndPutJobInCacheForAllFields() throws Exception {
        return createAndPutJobInCache();
    }

    public IdMappingJob createAndPutJobInCache(String from, String to, String fromIds)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        Map<String, String> mappedIds =
                Arrays.stream(fromIds.split(","))
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        this::getValue,
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        return createAndPutJobInCache(from, to, mappedIds);
    }

    private String getValue(String value) {
        String result = value;
        int subSequenceIndex = value.indexOf(SEQ_SEP);
        if (subSequenceIndex > 0) {
            result = value.substring(0, subSequenceIndex);
        } else if (value.indexOf(VERSION_SEP) > 0) {
            result = value.substring(0, value.indexOf(VERSION_SEP));
        } else if (value.indexOf(ID_SEP) > 0) {
            if (UNIPROTKB_ACCESSION_REGEX.matcher(value.split(ID_SEP)[0]).matches()) {
                result = value.substring(0, value.indexOf(ID_SEP));
            }
        }
        return result;
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
        String jobId = generateHash();
        IdMappingResult idMappingResult = createIdMappingResult(idMappingRequest, mappedIds);
        IdMappingJob job = createJob(jobId, idMappingRequest, idMappingResult, jobStatus);
        if (!this.cacheService.exists(jobId)) {
            this.cacheService.put(jobId, job); // put the finished job in cache
        }
        return job;
    }

    public IdMappingJobRequest createRequest(String from, String to, String fromIds) {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setIds(fromIds);
        return request;
    }

    private String generateHash() {
        return UUID.randomUUID().toString();
    }

    private IdMappingResult createIdMappingResult(
            IdMappingJobRequest request, Map<String, String> mappedIds) {
        List<IdMappingStringPair> ids =
                mappedIds.entrySet().stream()
                        .flatMap(
                                entry ->
                                        Arrays.stream(entry.getValue().split(";"))
                                                .map(
                                                        to ->
                                                                new IdMappingStringPair(
                                                                        entry.getKey(), to)))
                        .collect(Collectors.toList());
        return IdMappingResult.builder().mappedIds(ids).build();
    }

    public IdMappingJob createJob(
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
