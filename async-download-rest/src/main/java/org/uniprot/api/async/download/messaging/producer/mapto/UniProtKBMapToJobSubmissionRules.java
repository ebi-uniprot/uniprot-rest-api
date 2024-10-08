package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class UniProtKBMapToJobSubmissionRules
        extends MapToJobSubmissionRules<UniProtKBToUniRefDownloadRequest> {
    protected UniProtKBMapToJobSubmissionRules(
            @Value("${async.download.mapto.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.mapto.waitingMaxTime}") int maxWaitingTime,
            MapToJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
