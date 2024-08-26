package org.uniprot.api.async.download.messaging.consumer.streamer.batch.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;

public abstract class MapSolrIdBatchResultStreamerTest<T extends DownloadRequest, S>
        extends SolrIdBatchResultStreamerTest<T, MapDownloadJob, S> {
    protected static final int BATCH_SIZE = 2;
    @Mock protected MapDownloadJob mapDownloadJob;
    @Mock protected MapHeartbeatProducer mapHeartbeatProducer;
    @Mock protected MapJobService mapJobService;
    @Mock protected StoreStreamerConfig<S> uniRefEntryStoreStreamerConfig;
    @Mock protected UniRefLightStoreClient uniRefLightStoreClient;
    @Mock protected StreamerConfigProperties streamerConfig;

    void init() {
        job = mapDownloadJob;
        heartbeatProducer = mapHeartbeatProducer;
        jobService = mapJobService;
    }
}
