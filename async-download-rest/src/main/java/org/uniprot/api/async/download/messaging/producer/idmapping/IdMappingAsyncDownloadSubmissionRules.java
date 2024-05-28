package org.uniprot.api.async.download.messaging.producer.idmapping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;

//TODO: Important: We need to verify if idmapping job (IdMappingJobCacheService) is completed before we start the download
@Component
public class IdMappingAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules {
    public IdMappingAsyncDownloadSubmissionRules(
            @Value("${async.download.idmapping.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.idmapping.waitingMaxTime}") int maxWaitingTime,
            IdMappingJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
