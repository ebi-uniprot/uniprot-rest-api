package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

public abstract class MapRDFResultStreamerTest<T extends MapToDownloadRequest>
        extends RDFResultStreamerTest<T, MapToDownloadJob> {
    @Mock protected MapToDownloadJob mapToDownloadJob;
    @Mock protected MapToHeartbeatProducer mapToHeartbeatProducer;
    @Mock protected MapToJobService mapToJobService;

    void init() {
        job = mapToDownloadJob;
        heartbeatProducer = mapToHeartbeatProducer;
        jobService = mapToJobService;
    }
}
