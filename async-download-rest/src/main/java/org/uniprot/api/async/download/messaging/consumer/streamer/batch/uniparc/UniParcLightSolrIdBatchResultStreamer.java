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
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcLightBatchStoreIterable;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcLightSolrIdBatchResultStreamer
        extends SolrIdBatchResultStreamer<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntryLight> {
    private final StoreStreamerConfig<UniParcEntryLight> storeStreamerConfig;
    private final UniParcCrossReferenceLazyLoader lazyLoader;

    public UniParcLightSolrIdBatchResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcJobService jobService,
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniParcCrossReferenceLazyLoader lazyLoader) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeLightStreamerConfig;
        this.lazyLoader = lazyLoader;
    }

    @Override
    public BatchStoreIterable<UniParcEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, UniParcDownloadRequest request) {
        return new UniParcLightBatchStoreIterable(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                this.lazyLoader,
                request.getFields());
    }
}
