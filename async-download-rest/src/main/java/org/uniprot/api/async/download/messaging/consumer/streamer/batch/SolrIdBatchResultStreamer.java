package org.uniprot.api.async.download.messaging.consumer.streamer.batch;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.IdResultStreamer;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SolrIdBatchResultStreamer<T extends DownloadRequest, R extends DownloadJob, U>
        extends IdResultStreamer<T, R, U> {
    private final HeartbeatProducer heartbeatProducer;

    protected SolrIdBatchResultStreamer(
            HeartbeatProducer heartbeatProducer, JobService<R> jobService) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
    }

    @Override
    public Stream<U> stream(T request, Stream<String> ids) {
        R job = getJob(request);
        Iterable<Collection<U>> batchStoreIterable = getBatchStoreIterable(ids.iterator(), request);

        return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                .peek(
                        entityCollection ->
                                heartbeatProducer.generateForResults(job, entityCollection.size()))
                .flatMap(Collection::stream)
                .onClose(
                        () ->
                                log.info(
                                        "Finished streaming entries for job {}",
                                        request.getDownloadJobId()));
    }

    protected abstract Iterable<Collection<U>> getBatchStoreIterable(
            Iterator<String> idsIterator, T request);
}
