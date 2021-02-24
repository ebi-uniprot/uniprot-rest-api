package org.uniprot.api.idmapping.service.job;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Slf4j
public class AsyncJobSubmitExceptionHandler implements AsyncUncaughtExceptionHandler {
    private final IdMappingJobCacheService cacheService;

    public AsyncJobSubmitExceptionHandler(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        for (Object param : params) {
            if (param instanceof IdMappingJob) {
                // update job with failed status
                updateJob((IdMappingJob) param, throwable);
            }
        }
    }

    private void updateJob(IdMappingJob job, Throwable throwable) {
        log.warn("Job failed {} ", job.getJobId());

        if (this.cacheService.exists(job.getJobId())) {
            IdMappingJob cachedJob = this.cacheService.get(job.getJobId());
            cachedJob.setJobStatus(JobStatus.ERROR);
            cachedJob.getErrorMessages().add(throwable.getMessage());
        } else {
            log.warn("Failed job not found in cache {}", job.getJobId());
        }
    }
}
