package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc;

import java.util.Iterator;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcBatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.UniProtStoreClient;

@Component
public class UniParcSolrIdBatchResultStreamer
        extends SolrIdBatchResultStreamer<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private final StoreStreamerConfig<UniParcEntryLight> storeStreamerConfig;
    private final UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossRefStoreClient;
    private final UniParcCrossReferenceStoreConfigProperties crossRefConfigProperties;

    public UniParcSolrIdBatchResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcJobService jobService,
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossReferenceStoreClient,
            UniParcCrossReferenceStoreConfigProperties crossRefConfigProperties) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeLightStreamerConfig;
        this.uniParcCrossRefStoreClient = uniParcCrossReferenceStoreClient;
        this.crossRefConfigProperties = crossRefConfigProperties;
    }

    @Override
    public BatchIterable<UniParcEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, UniParcDownloadRequest request) {
        return new UniParcBatchStoreIterable(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                this.uniParcCrossRefStoreClient,
                this.crossRefConfigProperties);
    }
}
