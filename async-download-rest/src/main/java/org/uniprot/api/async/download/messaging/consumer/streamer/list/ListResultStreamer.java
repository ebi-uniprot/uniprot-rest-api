package org.uniprot.api.async.download.messaging.consumer.streamer.list;

import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.IdIdResultStreamer;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class ListResultStreamer<T extends DownloadRequest, R extends DownloadJob>
        extends IdIdResultStreamer<T, R> {

    private final HeartbeatProducer heartbeatProducer;

    protected ListResultStreamer(HeartbeatProducer heartbeatProducer, JobService<R> jobService) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
    }

    @Override
    public Stream<String> stream(T request, Stream<String> ids) {
        R job = getJob(request);
        return ids.map(
                id -> {
                    heartbeatProducer.createForResults(job, 1);
                    return id;
                });
    }
}
