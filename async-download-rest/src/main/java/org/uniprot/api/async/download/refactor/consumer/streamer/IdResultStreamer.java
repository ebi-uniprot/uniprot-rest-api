package org.uniprot.api.async.download.refactor.consumer.streamer;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

public abstract class IdResultStreamer<T extends DownloadRequest, R extends DownloadJob, P>
        extends ResultStreamer<T, R, String, P> {
    protected IdResultStreamer(JobService<R> jobService) {
        super(jobService);
    }
}
