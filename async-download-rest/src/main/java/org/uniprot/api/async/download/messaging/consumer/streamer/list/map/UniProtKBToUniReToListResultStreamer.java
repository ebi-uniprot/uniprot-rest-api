package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class UniProtKBToUniReToListResultStreamer
        extends UniRefMapToListResultStreamer<UniProtKBToUniRefDownloadRequest> {

    public UniProtKBToUniReToListResultStreamer(
            MapToHeartbeatProducer heartbeatProducer, MapToJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
