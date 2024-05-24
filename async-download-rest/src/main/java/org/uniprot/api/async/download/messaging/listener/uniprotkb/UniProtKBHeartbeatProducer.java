package org.uniprot.api.async.download.messaging.listener.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBHeartbeatProducer extends HeartbeatProducer {
    public UniProtKBHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, UniProtKBJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
