package org.uniprot.api.async.download.messaging.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class IdMappingFileHandler extends FileHandler {
    public IdMappingFileHandler(
            IdMappingDownloadConfigProperties idMappingDownloadConfigProperties,
            IdMappingHeartbeatProducer idMappingHeartbeatProducer) {
        super(idMappingDownloadConfigProperties, idMappingHeartbeatProducer);
    }
}
