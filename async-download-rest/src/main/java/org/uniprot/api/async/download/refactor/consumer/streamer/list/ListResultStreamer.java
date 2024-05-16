package org.uniprot.api.async.download.refactor.consumer.streamer.list;

import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.IdIdResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.IdResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.async.download.refactor.consumer.streamer.ResultStreamer;

import java.util.stream.Stream;

public abstract class ListResultStreamer<T extends DownloadRequest, R extends DownloadJob> extends IdIdResultStreamer<T, R> {

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
