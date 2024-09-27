package org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class MapToHeartbeatProducer extends HeartbeatProducer {
    public MapToHeartbeatProducer(HeartbeatConfig heartbeatConfig, MapToJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
