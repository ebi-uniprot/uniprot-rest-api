package org.uniprot.api.async.download.messaging.consumer.streamer.list.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class UniProtKBToUniRefToListResultStreamer
        extends UniRefMapToListResultStreamer<UniProtKBToUniRefDownloadRequest> {

    public UniProtKBToUniRefToListResultStreamer(
            MapToHeartbeatProducer heartbeatProducer, MapToJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
