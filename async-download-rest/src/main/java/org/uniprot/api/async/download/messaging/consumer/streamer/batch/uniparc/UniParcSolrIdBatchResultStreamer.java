package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniparc.UniParcEntry;

@Component
public class UniParcSolrIdBatchResultStreamer
        extends SolrIdBatchResultStreamer<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private final StoreStreamerConfig<UniParcEntry> storeStreamerConfig;

    public UniParcSolrIdBatchResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcJobService jobService,
            StoreStreamerConfig<UniParcEntry> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreIterable<UniParcEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, UniParcDownloadRequest request) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }
}
