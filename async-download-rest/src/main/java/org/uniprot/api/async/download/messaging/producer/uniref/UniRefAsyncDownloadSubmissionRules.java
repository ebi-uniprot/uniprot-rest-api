package org.uniprot.api.async.download.messaging.producer.uniref;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;

@Component
public class UniRefAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules {
    public UniRefAsyncDownloadSubmissionRules(
            @Value("${async.download.uniref.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.uniref.waitingMaxTime}") int maxWaitingTime,
            UniRefDownloadJobRepository downloadJobRepository) {
        super(maxRetryCount, maxWaitingTime, downloadJobRepository);
    }
}