package org.uniprot.api.async.download.messaging.consumer.streamer.batch;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.ResultStreamer;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class IdMappingBatchResultStreamer<Q, P extends EntryPair<Q>>
        extends ResultStreamer<
                IdMappingDownloadRequest, IdMappingDownloadJob, IdMappingStringPair, P> {
    private final HeartbeatProducer heartbeatProducer;

    protected IdMappingBatchResultStreamer(
            HeartbeatProducer heartbeatProducer, IdMappingJobService jobService) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
    }

    @Override
    public Stream<P> stream(IdMappingDownloadRequest request, Stream<IdMappingStringPair> ids) {
        IdMappingDownloadJob job = getJob(request);
        BatchStoreEntryPairIterable<P, Q> batchStoreEntryPairIterable =
                getBatchStoreEntryPairIterable(ids.iterator(), request);
        return StreamSupport.stream(batchStoreEntryPairIterable.spliterator(), false)
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

    protected abstract BatchStoreEntryPairIterable<P, Q> getBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> idsIterator, IdMappingDownloadRequest request);
}
