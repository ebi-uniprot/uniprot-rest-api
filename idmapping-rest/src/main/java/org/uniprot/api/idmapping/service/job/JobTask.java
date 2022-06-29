package org.uniprot.api.idmapping.service.job;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.core.util.Utils;

import com.google.common.base.Stopwatch;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Slf4j
public class JobTask implements Runnable {
    private static final int REST_EXCEPTION_CODE = 50;
    private final IdMappingJob job;
    private final IdMappingPIRService pirService;
    private final IdMappingJobCacheService cacheService;

    public JobTask(IdMappingJob job, IdMappingPIRService pirService, IdMappingJobCacheService cacheService) {
        this.job = job;
        this.pirService = pirService;
        this.cacheService = cacheService;
    }

    @Override
    public void run() {
        this.job.setJobStatus(JobStatus.RUNNING);
        this.job.setUpdated(new Date());
        this.cacheService.put(this.job.getJobId(), this.job);
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            IdMappingResult pirResponse =
                    pirService.mapIds(this.job.getIdMappingRequest(), job.getJobId());
            stopwatch.stop();

            log.info(
                    "[idmapping/run/{}] response took {} seconds (id count={})",
                    job.getJobId(),
                    stopwatch.elapsed(TimeUnit.SECONDS),
                    job.getIdMappingRequest().getIds().split(",").length);

            if (Utils.notNullNotEmpty(
                    pirResponse.getErrors())) { //  app constraint violation limit exceed
                populateError(pirResponse.getErrors());
                this.job.setIdMappingResult(pirResponse);
            } else { // no app constraint violation
                this.job.setJobStatus(JobStatus.FINISHED);
                this.job.setIdMappingResult(pirResponse);
                this.job.setUpdated(new Date());
            }
        } catch (RestClientException restException) {
            populateError(
                    List.of(new ProblemPair(REST_EXCEPTION_CODE, restException.getMessage())));
        } finally {
            this.cacheService.put(this.job.getJobId(), this.job);
        }
    }

    private void populateError(List<ProblemPair> errors) {
        this.job.setErrors(errors);
        this.job.setJobStatus(JobStatus.ERROR);
        this.job.setUpdated(new Date());
    }
}
