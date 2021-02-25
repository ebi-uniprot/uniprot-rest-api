package org.uniprot.api.idmapping.service.job;

import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 23/02/2021
 */
public class JobTask implements Runnable {
    private final IdMappingJobCacheService cacheService;
    private final IdMappingJob job;
    private final IdMappingPIRService pirService;

    public JobTask(
            IdMappingJob job,
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService) {
        this.job = job;
        this.cacheService = cacheService;
        this.pirService = pirService;
    }

    @Override
    public void run() {
        this.job.setJobStatus(JobStatus.RUNNING);
        this.cacheService.put(job.getJobId(), job);
        IdMappingResult pirResponse = pirService.mapIds(job.getIdMappingRequest());
        job.setJobStatus(JobStatus.FINISHED);
        job.setIdMappingResult(pirResponse);
        this.cacheService.put(job.getJobId(), job);
    }
}
