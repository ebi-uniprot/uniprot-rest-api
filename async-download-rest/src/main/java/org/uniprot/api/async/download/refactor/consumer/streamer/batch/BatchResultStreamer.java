package org.uniprot.api.async.download.refactor.consumer.streamer.batch;

import lombok.extern.slf4j.Slf4j;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.IdResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.ResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class BatchResultStreamer<T extends DownloadRequest, R extends DownloadJob, S> extends IdResultStreamer<T, R, S> {
    private final HeartbeatProducer heartbeatProducer;

    protected BatchResultStreamer(HeartbeatProducer heartbeatProducer, JobService<R> jobService) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
    }

    @Override
    public Stream<S> stream(T request, Stream<String> ids) {
        R job = getJob(request);
        Iterable<Collection<S>> batchStoreIterable = getBatchStoreIterable(ids.iterator(), request);

        return StreamSupport.stream(batchStoreIterable.spliterator(), false)
                .peek(
                        entityCollection -> heartbeatProducer.createForResults(
                                job, entityCollection.size()))
                .flatMap(Collection::stream)
                .onClose(
                        () ->
                                log.info(
                                        "Finished streaming entries for job {}",
                                        request.getJobId()));
    }

    protected abstract Iterable<Collection<S>> getBatchStoreIterable(Iterator<String> idsIterator, T request);
}
