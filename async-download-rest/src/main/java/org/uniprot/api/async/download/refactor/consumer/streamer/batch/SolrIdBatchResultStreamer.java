package org.uniprot.api.async.download.refactor.consumer.streamer.batch;

import lombok.extern.slf4j.Slf4j;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.IdResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class SolrIdBatchResultStreamer<T extends DownloadRequest, R extends DownloadJob, P> extends IdResultStreamer<T, R, P> {
    private final HeartbeatProducer heartbeatProducer;

    protected SolrIdBatchResultStreamer(HeartbeatProducer heartbeatProducer, JobService<R> jobService) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
    }

    @Override
    public Stream<P> stream(T request, Stream<String> ids) {
        R job = getJob(request);
        Iterable<Collection<P>> batchStoreIterable = getBatchStoreIterable(ids.iterator(), request);

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

    protected abstract Iterable<Collection<P>> getBatchStoreIterable(Iterator<String> idsIterator, T request);
}
