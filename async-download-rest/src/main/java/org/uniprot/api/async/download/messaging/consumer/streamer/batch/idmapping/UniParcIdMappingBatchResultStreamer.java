package org.uniprot.api.async.download.messaging.consumer.streamer.batch.idmapping;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniParcLightBatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcIdMappingBatchResultStreamer
        extends IdMappingBatchResultStreamer<UniParcEntryLight, UniParcEntryLightPair> {
    protected final StoreStreamerConfig<UniParcEntryLight> storeStreamerConfig;
    private final UniParcCrossReferenceLazyLoader lazyLoader;

    protected UniParcIdMappingBatchResultStreamer(
            IdMappingHeartbeatProducer heartbeatProducer,
            IdMappingJobService jobService,
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniParcCrossReferenceLazyLoader lazyLoader) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeLightStreamerConfig;
        this.lazyLoader = lazyLoader;
    }

    @Override
    public BatchStoreEntryPairIterable<UniParcEntryLightPair, UniParcEntryLight>
            getBatchStoreEntryPairIterable(
                    Iterator<IdMappingStringPair> mappedIds, IdMappingDownloadRequest request) {
        return new UniParcLightBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                this.lazyLoader,
                request.getFields());
    }
}
