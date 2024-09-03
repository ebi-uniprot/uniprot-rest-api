package org.uniprot.api.async.download.messaging.producer.map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

@Component
public class UniProtKBMapJobSubmissionRules
        extends MapJobSubmissionRules<UniProtKBToUniRefMapDownloadRequest> {
    protected UniProtKBMapJobSubmissionRules(
            @Value("${async.download.map.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.map.waitingMaxTime}") int maxWaitingTime,
            MapJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
