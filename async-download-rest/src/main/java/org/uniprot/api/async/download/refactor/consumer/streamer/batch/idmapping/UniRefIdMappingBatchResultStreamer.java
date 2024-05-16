package org.uniprot.api.async.download.refactor.consumer.streamer.batch.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniRefBatchStoreEntryPairIterable;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.util.Iterator;

@Component
public class UniRefIdMappingBatchResultStreamer extends IdMappingBatchResultStreamer<UniRefEntryLight, UniRefEntryPair> {
    protected final StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;

    protected UniRefIdMappingBatchResultStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService, StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight>
    getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, IdMappingDownloadRequest request) {
        return new UniRefBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy());
    }
}
