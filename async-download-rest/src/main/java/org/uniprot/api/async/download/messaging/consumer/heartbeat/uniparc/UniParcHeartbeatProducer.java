package org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcHeartbeatProducer extends HeartbeatProducer {
    public UniParcHeartbeatProducer(HeartbeatConfig heartbeatConfig, UniParcJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
