package org.uniprot.api.async.download.messaging.producer.map;

import org.uniprot.api.async.download.messaging.producer.JobSubmissionRules;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class MapJobSubmissionRules<T extends MapDownloadRequest>
        extends JobSubmissionRules<T, MapDownloadJob> {
    protected MapJobSubmissionRules(
            int maxRetryCount, int maxWaitingTime, JobService<MapDownloadJob> jobService) {
        super(maxRetryCount, maxWaitingTime, jobService);
    }
}
