package org.uniprot.api.async.download.messaging.producer.uniref;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@Component
public class UniRefAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules {
    public UniRefAsyncDownloadSubmissionRules(
            @Value("${async.download.uniref.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.uniref.waitingMaxTime}") int maxWaitingTime,
            UniRefJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
