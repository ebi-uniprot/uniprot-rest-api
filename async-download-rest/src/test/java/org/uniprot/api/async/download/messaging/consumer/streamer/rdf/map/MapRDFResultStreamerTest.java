package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class MapRDFResultStreamerTest<T extends MapDownloadRequest>
        extends RDFResultStreamerTest<T, MapDownloadJob> {
    @Mock protected MapDownloadJob mapDownloadJob;
    @Mock protected MapHeartbeatProducer mapHeartbeatProducer;
    @Mock protected MapJobService mapJobService;

    void init() {
        job = mapDownloadJob;
        heartbeatProducer = mapHeartbeatProducer;
        jobService = mapJobService;
    }
}
