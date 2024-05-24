package org.uniprot.api.async.download.messaging.listener.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;

@Component
public class IdMappingHeartbeatProducer extends HeartbeatProducer {
    public IdMappingHeartbeatProducer(
            HeartbeatConfig heartbeatConfig, IdMappingJobService jobService) {
        super(heartbeatConfig, jobService);
    }
}
