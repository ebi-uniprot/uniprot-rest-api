package org.uniprot.api.async.download.refactor.consumer.streamer.batch.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniParcBatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;

import java.util.Iterator;

@Component
public class UniParcIdMappingBatchResultStreamer extends IdMappingBatchResultStreamer<UniParcEntry, UniParcEntryPair> {
    protected final StoreStreamerConfig<UniParcEntry> storeStreamerConfig;

    protected UniParcIdMappingBatchResultStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService, StoreStreamerConfig<UniParcEntry> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    public BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry>
    getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, IdMappingDownloadRequest request) {
        return new UniParcBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy());
    }
}
