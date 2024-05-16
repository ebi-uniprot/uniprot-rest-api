package org.uniprot.api.async.download.refactor.consumer.streamer;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

public abstract class IdIdResultStreamer<T extends DownloadRequest, R extends DownloadJob>
        extends IdResultStreamer<T, R, String> {
    protected IdIdResultStreamer(JobService<R> jobService) {
        super(jobService);
    }
}
