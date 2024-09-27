package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class UniProtKBMapToJobSubmissionRules
        extends MapToJobSubmissionRules<UniProtKBToUniRefDownloadRequest> {
    protected UniProtKBMapToJobSubmissionRules(
            @Value("${async.download.map.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.map.waitingMaxTime}") int maxWaitingTime,
            MapToJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
