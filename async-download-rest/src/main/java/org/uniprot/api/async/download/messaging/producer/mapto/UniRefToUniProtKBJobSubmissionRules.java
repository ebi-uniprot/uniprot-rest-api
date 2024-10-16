package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.request.mapto.UniRefToUniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class UniRefToUniProtKBJobSubmissionRules
        extends MapToJobSubmissionRules<UniRefToUniProtKBDownloadRequest> {
    protected UniRefToUniProtKBJobSubmissionRules(
            @Value("${async.download.mapto.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.mapto.waitingMaxTime}") int maxWaitingTime,
            MapToJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
