package org.uniprot.api.async.download.refactor.consumer.streamer;

import java.util.stream.Stream;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

public abstract class ResultStreamer<T extends DownloadRequest, R extends DownloadJob, S, P> {
    private final JobService<R> jobService;

    protected ResultStreamer(JobService<R> jobService) {
        this.jobService = jobService;
    }

    public abstract Stream<P> stream(T request, Stream<S> ids);

    protected R getJob(T request) {
        String jobId = request.getJobId();
        return jobService
                .find(jobId)
                .orElseThrow(
                        () -> new IllegalArgumentException("job id %s not found".formatted(jobId)));
    }
}
