package org.uniprot.api.async.download.messaging.producer.uniref;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@Component
public class UniRefAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules<UniRefDownloadRequest, UniRefDownloadJob> {
    public UniRefAsyncDownloadSubmissionRules(
            @Value("${async.download.uniref.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.uniref.waitingMaxTime}") int maxWaitingTime,
            UniRefJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
