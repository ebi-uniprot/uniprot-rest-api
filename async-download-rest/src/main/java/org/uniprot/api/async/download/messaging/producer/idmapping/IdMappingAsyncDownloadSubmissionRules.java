package org.uniprot.api.async.download.messaging.producer.idmapping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;

@Component
@Profile({"asyncDownload"})
public class IdMappingAsyncDownloadSubmissionRules extends AsyncDownloadSubmissionRules {
    public IdMappingAsyncDownloadSubmissionRules(
            @Value("${async.download.idmapping.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.idmapping.waitingMaxTime}") int maxWaitingTime,
            DownloadJobRepository downloadJobRepository) {
        super(maxRetryCount, maxWaitingTime, downloadJobRepository);
    }
}
