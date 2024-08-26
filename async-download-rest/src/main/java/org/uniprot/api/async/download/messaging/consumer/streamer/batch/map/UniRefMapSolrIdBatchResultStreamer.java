package org.uniprot.api.async.download.messaging.consumer.streamer.batch.map;

import java.util.Iterator;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniref.UniRefEntryLight;

public abstract class UniRefMapSolrIdBatchResultStreamer<T extends MapDownloadRequest>
        extends SolrIdBatchResultStreamer<T, MapDownloadJob, UniRefEntryLight> {
    private final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    protected UniRefMapSolrIdBatchResultStreamer(
            MapHeartbeatProducer heartbeatProducer,
            MapJobService jobService,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreIterable<UniRefEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, T request) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }
}
