package org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBHeartbeatProducer extends HeartbeatProducer {
    public UniProtKBHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, UniProtKBJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
