package org.uniprot.api.async.download.messaging.consumer.streamer;

import java.util.stream.Stream;

import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class ResultStreamer<T extends DownloadRequest, R extends DownloadJob, S, U> {
    private final JobService<R> jobService;

    protected ResultStreamer(JobService<R> jobService) {
        this.jobService = jobService;
    }

    public abstract Stream<U> stream(T request, Stream<S> ids);

    protected R getJob(T request) {
        String id = request.getId();
        return jobService
                .find(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("job id %s not found".formatted(id)));
    }
}
