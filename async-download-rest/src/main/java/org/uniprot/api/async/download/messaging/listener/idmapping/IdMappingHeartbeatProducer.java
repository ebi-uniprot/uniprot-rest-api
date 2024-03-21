package org.uniprot.api.async.download.messaging.listener.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;

@Component
public class IdMappingHeartbeatProducer extends HeartbeatProducer {
    public IdMappingHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, IdMappingDownloadJobRepository jobRepository) {
        super(heartbeatConfig, jobRepository);
    }
}
