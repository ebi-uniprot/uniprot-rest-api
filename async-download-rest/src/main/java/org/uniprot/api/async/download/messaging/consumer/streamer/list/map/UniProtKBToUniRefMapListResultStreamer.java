package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

@Component
public class UniProtKBToUniRefMapListResultStreamer
        extends UniRefMapListResultStreamer<UniProtKBMapDownloadRequest> {

    public UniProtKBToUniRefMapListResultStreamer(
            MapHeartbeatProducer heartbeatProducer, MapJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
