package org.uniprot.api.async.download.messaging.consumer.streamer.batch.map;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefMapSolrIdBatchResultStreamer
        extends UniRefMapSolrIdBatchResultStreamer<UniProtKBToUniRefMapDownloadRequest> {
    private final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    public UniProtKBToUniRefMapSolrIdBatchResultStreamer(
            MapHeartbeatProducer heartbeatProducer,
            MapJobService jobService,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        super(heartbeatProducer, jobService, storeStreamerConfig);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreIterable<UniRefEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, UniProtKBToUniRefMapDownloadRequest request) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }
}
