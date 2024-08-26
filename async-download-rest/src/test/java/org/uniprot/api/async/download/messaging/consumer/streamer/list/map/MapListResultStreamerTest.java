package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamerTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniref.UniRefListResultStreamer;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class MapListResultStreamerTest<T extends MapDownloadRequest>
        extends ListResultStreamerTest<T, MapDownloadJob> {
    @Mock protected MapDownloadJob mapDownloadJob;
    @Mock protected MapHeartbeatProducer mapHeartbeatProducer;
    @Mock protected MapJobService mapJobService;

    void init() {
        job = mapDownloadJob;
        heartbeatProducer = mapHeartbeatProducer;
        jobService = mapJobService;
    }
}
