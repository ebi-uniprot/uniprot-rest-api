package org.uniprot.api.async.download.messaging.consumer.heartbeat.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class MapHeartbeatProducer extends HeartbeatProducer {
    public MapHeartbeatProducer(HeartbeatConfig heartbeatConfig, MapJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
