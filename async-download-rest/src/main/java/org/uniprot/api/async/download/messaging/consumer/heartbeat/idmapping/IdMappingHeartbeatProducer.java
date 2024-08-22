package org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@Component
public class IdMappingHeartbeatProducer extends HeartbeatProducer {
    public IdMappingHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, IdMappingJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
