package org.uniprot.api.async.download.messaging.consumer.streamer;

import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class IdIdResultStreamer<T extends DownloadRequest, R extends DownloadJob>
        extends IdResultStreamer<T, R, String> {
    protected IdIdResultStreamer(JobService<R> jobService) {
        super(jobService);
    }
}
