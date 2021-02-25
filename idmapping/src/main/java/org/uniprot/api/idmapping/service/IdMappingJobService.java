package org.uniprot.api.idmapping.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobStatusResponse;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.job.AsyncJobProducer;
import org.uniprot.api.idmapping.service.job.JobTask;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Service
public class IdMappingJobService {

    private final IdMappingJobCacheService cacheService;
    private final BlockingQueue<IdMappingJob> jobQueue;
    private final IdMappingPIRService pirService;
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final HashGenerator hashGenerator;
    private final AsyncJobProducer jobProducer;

    public IdMappingJobService(
            IdMappingJobCacheService cacheService,
            AsyncJobProducer asyncJobProducer,
            BlockingQueue<IdMappingJob> jobQueue,
            IdMappingPIRService pirService,
            ThreadPoolTaskExecutor jobTaskExecutor) {
        this.cacheService = cacheService;
        this.jobQueue = jobQueue;
        this.pirService = pirService;
        this.jobTaskExecutor = jobTaskExecutor;
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

            // create task and submit
            JobTask jobTask = new JobTask(jobQueue, cacheService, pirService);
            jobTaskExecutor.execute(jobTask);
        } else {
            IdMappingJob job = this.cacheService.get(jobId);

            // update expiry time
            job.setUpdated(new Date());
        }

        return new JobSubmitResponse(jobId);
    }

    public ResponseEntity<JobStatusResponse> getStatus(String jobId) {
        ResponseEntity<JobStatusResponse> response;
        if (this.cacheService.exists(jobId)) {
            IdMappingJob job = this.cacheService.get(jobId);
            switch (job.getJobStatus()) {
                case NEW:
                case RUNNING:
                    response = ResponseEntity.ok(new JobStatusResponse(job.getJobStatus()));
                    break;
                case FINISHED:
                    String redirectUrl = getRedirectPathToResults(job);

                    response =
                            ResponseEntity.status(HttpStatus.SEE_OTHER)
                                    .header(HttpHeaders.LOCATION, redirectUrl)
                                    .body(new JobStatusResponse(JobStatus.FINISHED));
                    break;
                default:
                    response =
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(new JobStatusResponse(JobStatus.ERROR));
                    break;
            }

        } else {
            response = ResponseEntity.notFound().build();
        }

        return response;
    }

    private String getRedirectPathToResults(IdMappingJob job) {
        String toDB = job.getIdMappingRequest().getTo();
        String dbType = "";
        if (Set.of("NF50", "NF90", "NF100").contains(toDB)) {
            dbType = "uniref/";
        } else if ("UPARC".equals(toDB)) {
            dbType = "uniparc/";
        } else if (Set.of("ACC", "ID", "SWISSPROT").contains(toDB)) {
            dbType = "uniprotkb/";
        }

        return "/uniprot/api/idmapping/" + dbType + "results/" + job.getJobId();
    }

    private IdMappingJob createJob(String jobId, IdMappingBasicRequest request) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(JobStatus.NEW);
        builder.idMappingRequest(request);
        return builder.build();
    }
}
