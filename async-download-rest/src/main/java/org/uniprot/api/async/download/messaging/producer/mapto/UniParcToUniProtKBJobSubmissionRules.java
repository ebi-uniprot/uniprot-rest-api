package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.UniParcToUniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.JobService;

@Component
public class UniParcToUniProtKBJobSubmissionRules extends MapToJobSubmissionRules<UniParcToUniProtKBMapDownloadRequest>{
    public UniParcToUniProtKBJobSubmissionRules(@Value("${async.download.mapto.retryMaxCount}") int maxRetryCount,
                                                @Value("${async.download.mapto.waitingMaxTime}") int maxWaitingTime,
                                                JobService<MapToDownloadJob> jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
