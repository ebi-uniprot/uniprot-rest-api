package org.uniprot.api.async.download.messaging.consumer.streamer.batch.mapto;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;

public abstract class MapSolrIdBatchResultStreamerTest<T extends DownloadRequest, S>
        extends SolrIdBatchResultStreamerTest<T, MapToDownloadJob, S> {
    protected static final int BATCH_SIZE = 2;
    @Mock protected MapToDownloadJob mapToDownloadJob;
    @Mock protected MapToHeartbeatProducer mapToHeartbeatProducer;
    @Mock protected MapToJobService mapToJobService;
    @Mock protected StoreStreamerConfig<S> uniRefEntryStoreStreamerConfig;
    @Mock protected UniRefLightStoreClient uniRefLightStoreClient;
    @Mock protected StreamerConfigProperties streamerConfig;

    void init() {
        job = mapToDownloadJob;
        heartbeatProducer = mapToHeartbeatProducer;
        jobService = mapToJobService;
    }
}
