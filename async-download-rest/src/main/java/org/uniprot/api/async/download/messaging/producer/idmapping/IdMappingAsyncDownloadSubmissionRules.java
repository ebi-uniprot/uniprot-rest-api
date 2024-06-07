package org.uniprot.api.async.download.messaging.producer.idmapping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.model.common.JobSubmitFeedback;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

import java.text.MessageFormat;
import java.util.Optional;

@Component
public class IdMappingAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules<IdMappingDownloadRequest, IdMappingDownloadJob> {
    private final IdMappingJobCacheService idMappingJobCacheService;

    public IdMappingAsyncDownloadSubmissionRules(
            @Value("${async.download.idmapping.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.idmapping.waitingMaxTime}") int maxWaitingTime,
            IdMappingJobService jobService, IdMappingJobCacheService idMappingJobCacheService) {
        super(maxRetryCount, maxWaitingTime, jobService);
        this.idMappingJobCacheService = idMappingJobCacheService;
    }

    @Override
    public JobSubmitFeedback submit(IdMappingDownloadRequest request) {
        String idMappingJobId = request.getJobId();
        IdMappingJob idMappingJob = Optional.ofNullable(idMappingJobCacheService.get(idMappingJobId)).orElseThrow(() -> new IllegalArgumentException("Invalid job id: %s".formatted(idMappingJobId)));
        if (idMappingJob.getIdMappingResult() != null) {
            return super.submit(request);
        }
        return new JobSubmitFeedback(false, MessageFormat.format("ID Mapping Job {0} id not yet finished", idMappingJobId));
    }
}
