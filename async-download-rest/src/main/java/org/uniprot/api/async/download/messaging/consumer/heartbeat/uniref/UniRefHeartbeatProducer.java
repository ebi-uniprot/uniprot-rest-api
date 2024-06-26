package org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@Component
public class UniRefHeartbeatProducer extends HeartbeatProducer {
    public UniRefHeartbeatProducer(HeartbeatConfig heartbeatConfig, UniRefJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
