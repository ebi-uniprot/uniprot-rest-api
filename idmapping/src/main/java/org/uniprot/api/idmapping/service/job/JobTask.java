package org.uniprot.api.idmapping.service.job;

import java.util.concurrent.BlockingQueue;

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
    private final BlockingQueue<IdMappingJob> queue;
    private final IdMappingPIRService pirService;

    public JobTask(
            BlockingQueue<IdMappingJob> queue,
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService) {
        this.queue = queue;
        this.cacheService = cacheService;
        this.pirService = pirService;
    }

    @Override
    public void run() {
        try {
            IdMappingJob job = this.queue.take();
            job.setJobStatus(JobStatus.RUNNING);
            this.cacheService.put(job.getJobId(), job);
            IdMappingResult pirResponse = pirService.mapIds(job.getIdMappingRequest());
            job.setJobStatus(JobStatus.FINISHED);
            job.setIdMappingResult(pirResponse);
            this.cacheService.put(job.getJobId(), job);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // TODO: 24/02/2021 FIX ME
        }
    }
}
