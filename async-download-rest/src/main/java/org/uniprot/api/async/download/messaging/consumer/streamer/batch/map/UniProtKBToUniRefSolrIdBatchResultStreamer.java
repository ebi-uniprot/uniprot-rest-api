package org.uniprot.api.async.download.messaging.consumer.streamer.batch.map;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefSolrIdBatchResultStreamer
        extends UniRefMapToSolrIdBatchResultStreamer<UniProtKBToUniRefDownloadRequest> {
    private final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    public UniProtKBToUniRefSolrIdBatchResultStreamer(
            MapToHeartbeatProducer heartbeatProducer,
            MapToJobService jobService,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        super(heartbeatProducer, jobService, storeStreamerConfig);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreIterable<UniRefEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, UniProtKBToUniRefDownloadRequest request) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }
}
