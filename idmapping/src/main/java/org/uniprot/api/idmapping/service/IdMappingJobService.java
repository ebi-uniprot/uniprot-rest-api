package org.uniprot.api.idmapping.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.job.AsyncJobProducer;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Service
public class IdMappingJobService {

    private final IdMappingJobCacheService cacheService;
    private final HashGenerator hashGenerator;
    private final BlockingQueue<IdMappingJob> queue;
    private final AsyncJobProducer jobProducer;

    public IdMappingJobService(
            IdMappingJobCacheService cacheService, BlockingQueue<IdMappingJob> queue,
            AsyncJobProducer asyncJobProducer) {
        this.cacheService = cacheService;
        this.queue = queue;
        this.hashGenerator = new HashGenerator();
        this.jobProducer = asyncJobProducer;
    }

    public JobSubmitResponse submitJob(IdMappingBasicRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {

        String jobId = this.hashGenerator.generateHash(request);
        IdMappingJob idMappingJob = createJob(jobId, request);

        if (!this.cacheService.exists(jobId)) {
            this.cacheService.put(jobId, idMappingJob);
            this.jobProducer.enqueueJob(idMappingJob); // Async call
        }

        return new JobSubmitResponse(jobId);
    }

    private IdMappingJob createJob(String jobId, IdMappingBasicRequest request) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(JobStatus.NEW);
        builder.idMappingRequest(request);
        return builder.build();
    }
}
