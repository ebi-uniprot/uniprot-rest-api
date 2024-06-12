package org.uniprot.api.async.download.messaging.consumer.streamer;

import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class IdResultStreamer<T extends DownloadRequest, R extends DownloadJob, U>
        extends ResultStreamer<T, R, String, U> {
    protected IdResultStreamer(JobService<R> jobService) {
        super(jobService);
    }
}
