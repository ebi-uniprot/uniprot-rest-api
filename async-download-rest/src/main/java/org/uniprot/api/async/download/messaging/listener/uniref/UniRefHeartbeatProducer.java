package org.uniprot.api.async.download.messaging.listener.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@Component
public class UniRefHeartbeatProducer extends HeartbeatProducer {
    public UniRefHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, UniRefJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
