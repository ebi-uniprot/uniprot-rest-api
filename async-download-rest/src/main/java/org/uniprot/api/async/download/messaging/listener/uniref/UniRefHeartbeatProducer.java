package org.uniprot.api.async.download.messaging.listener.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@Component
public class UniRefHeartbeatProducer extends HeartbeatProducer {
    public UniRefHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, UniProtKBJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
