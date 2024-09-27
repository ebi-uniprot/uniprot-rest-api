package org.uniprot.api.async.download.messaging.producer.mapto;

import org.uniprot.api.async.download.messaging.producer.JobSubmissionRules;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class MapToJobSubmissionRules<T extends MapToDownloadRequest>
        extends JobSubmissionRules<T, MapToDownloadJob> {
    protected MapToJobSubmissionRules(
            int maxRetryCount, int maxWaitingTime, JobService<MapToDownloadJob> jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
