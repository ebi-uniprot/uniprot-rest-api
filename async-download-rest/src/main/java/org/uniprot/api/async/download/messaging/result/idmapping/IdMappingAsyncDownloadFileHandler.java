package org.uniprot.api.async.download.messaging.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;

@Component
public class IdMappingAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public IdMappingAsyncDownloadFileHandler(
            IdMappingDownloadConfigProperties idMappingDownloadConfigProperties, IdMappingHeartbeatProducer idMappingHeartbeatProducer) {
        super(idMappingDownloadConfigProperties, idMappingHeartbeatProducer);
    }
}
