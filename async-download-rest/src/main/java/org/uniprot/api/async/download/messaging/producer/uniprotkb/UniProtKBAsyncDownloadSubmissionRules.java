package org.uniprot.api.async.download.messaging.producer.uniprotkb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public UniProtKBAsyncDownloadSubmissionRules(
            @Value("${async.download.uniprotkb.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.uniprotkb.waitingMaxTime}") int maxWaitingTime,
            UniProtKBJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
