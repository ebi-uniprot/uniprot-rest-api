package org.uniprot.api.async.download.messaging.producer.uniparc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRules;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcJobSubmissionRules
        extends JobSubmissionRules<UniParcDownloadRequest, UniParcDownloadJob> {
    public UniParcJobSubmissionRules(
            @Value("${async.download.uniparc.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.uniparc.waitingMaxTime}") int maxWaitingTime,
            UniParcJobService jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
