package org.uniprot.api.async.download.messaging.producer.idmapping;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;

@Component
public class IdMappingAsyncDownloadSubmissionRules
        extends AsyncDownloadSubmissionRules<IdMappingDownloadRequest, IdMappingDownloadJob> {
    private final IdMappingJobCacheService idMappingJobCacheService;

    public IdMappingAsyncDownloadSubmissionRules(
            @Value("${async.download.idmapping.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.idmapping.waitingMaxTime}") int maxWaitingTime,
            IdMappingJobService jobService,
            IdMappingJobCacheService idMappingJobCacheService) {
        super(maxRetryCount, maxWaitingTime, jobService);
        this.idMappingJobCacheService = idMappingJobCacheService;
    }

    @Override
    public JobSubmitFeedback submit(IdMappingDownloadRequest request) {
        String idMappingJobId = request.getJobId();
        IdMappingJob idMappingJob = idMappingJobCacheService.get(idMappingJobId);
        String message;
        if (idMappingJob != null) {
            if (JobStatus.FINISHED.equals(idMappingJob.getJobStatus())) {
                return super.submit(request);
            } else {
                message = "ID Mapping Job id {0} not yet finished";
            }
        } else {
            message = "ID Mapping Job id {0} not found";
        }
        return new JobSubmitFeedback(false, MessageFormat.format(message, idMappingJobId));
    }
}
