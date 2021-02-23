package org.uniprot.api.idmapping.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.request.IdMappingRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Service
public class IdMappingJobService {

    private final IdMappingJobCacheService cacheService;
    private final HashGenerator hashGenerator;

    public IdMappingJobService(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
        this.hashGenerator = new HashGenerator();
    }

    public JobSubmitResponse submitJob(IdMappingRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        String jobId = this.hashGenerator.generateHash(request);
        IdMappingJob idMappingJob = createJob(jobId, request);

        if (!this.cacheService.exists(jobId)) {
            this.cacheService.put(jobId, idMappingJob);
        }

        // TODO logic to put idMappingJob in queue

        return new JobSubmitResponse(jobId);
    }

    private IdMappingJob createJob(String jobId, IdMappingRequest request) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(JobStatus.NEW);
        builder.idMappingRequest(request);
        return builder.build();
    }
}
