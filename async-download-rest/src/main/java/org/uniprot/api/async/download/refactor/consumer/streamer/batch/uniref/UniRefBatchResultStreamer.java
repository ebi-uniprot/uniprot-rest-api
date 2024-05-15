package org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniref;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.BatchResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefBatchResultStreamer
        extends BatchResultStreamer<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    private final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    public UniRefBatchResultStreamer(
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
