package org.uniprot.api.async.download.refactor.consumer.streamer;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import java.util.stream.Stream;

public abstract class ResultStreamer<T extends DownloadRequest, R extends DownloadJob, S, P> {
    private final JobService<R> jobService;

    protected ResultStreamer(JobService<R> jobService) {
        this.jobService = jobService;
    }

    public abstract Stream<S> stream(T request, Stream<String> ids);

    protected R getJob(T request) {
        String jobId = request.getJobId();
        return jobService.find(jobId).orElseThrow(() -> new IllegalArgumentException("job id %s not found".formatted(jobId)));
    }
}
