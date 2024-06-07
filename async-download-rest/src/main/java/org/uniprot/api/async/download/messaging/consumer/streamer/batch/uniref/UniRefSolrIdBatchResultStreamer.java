package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniref;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefSolrIdBatchResultStreamer
        extends SolrIdBatchResultStreamer<
                        UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    private final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    public UniRefSolrIdBatchResultStreamer(
            UniRefHeartbeatProducer heartbeatProducer,
            UniRefJobService jobService,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreIterable<UniRefEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, UniRefDownloadRequest request) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }
}
