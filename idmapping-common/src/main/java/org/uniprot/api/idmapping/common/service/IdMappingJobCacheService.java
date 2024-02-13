package org.uniprot.api.idmapping.common.service;

import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * Created 23/02/2021
 *
 * @author sahmad
 */
public interface IdMappingJobCacheService {
    void put(String key, IdMappingJob value);

    IdMappingJob get(String key);

    boolean exists(String key);

    void delete(String key);

    default IdMappingJob getJobAsResource(String jobId) {
        if (exists(jobId)) {
            return get(jobId);
        } else {
            throw new ResourceNotFoundException("{search.not.found}");
        }
    }

    default IdMappingJob getCompletedJobAsResource(String jobId) {
        IdMappingJob job = getJobAsResource(jobId);
        if (job.getJobStatus() == JobStatus.FINISHED) {
            return job;
        } else {
            throw new ResourceNotFoundException("{search.not.found}");
        }
    }
}
