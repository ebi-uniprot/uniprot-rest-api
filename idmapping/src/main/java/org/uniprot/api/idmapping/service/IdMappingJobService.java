package org.uniprot.api.idmapping.service;

import static org.uniprot.api.idmapping.controller.response.JobStatus.FINISHED;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Set;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.job.JobTask;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Service
public class IdMappingJobService {
    private final IdMappingJobCacheService cacheService;
    private final IdMappingPIRService pirService;
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final HashGenerator hashGenerator;

    public IdMappingJobService(
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService,
            ThreadPoolTaskExecutor jobTaskExecutor) {
        this.cacheService = cacheService;
        this.pirService = pirService;
        this.jobTaskExecutor = jobTaskExecutor;
        this.hashGenerator = new HashGenerator();
    }

    public JobSubmitResponse submitJob(IdMappingBasicRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException {

        String jobId = this.hashGenerator.generateHash(request);
        IdMappingJob idMappingJob = createJob(jobId, request);

        if (!this.cacheService.exists(jobId)) {
            this.cacheService.put(jobId, idMappingJob);
            // create task and submit
            JobTask jobTask = new JobTask(idMappingJob, cacheService, pirService);
            jobTaskExecutor.execute(jobTask);
        } else {
            IdMappingJob job = this.cacheService.get(jobId);

            // update expiry time
            job.setUpdated(new Date());
        }

        return new JobSubmitResponse(jobId);
    }

    public String getRedirectPathToResults(IdMappingJob job) {
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
