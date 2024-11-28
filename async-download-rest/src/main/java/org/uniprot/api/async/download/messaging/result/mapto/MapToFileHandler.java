package org.uniprot.api.async.download.messaging.result.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.mapto.MapToDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class MapToFileHandler extends FileHandler {
    public MapToFileHandler(
            MapToDownloadConfigProperties uniParcDownloadConfigProperties,
            MapToHeartbeatProducer uniParcHeartbeatProducer) {
        super(uniParcDownloadConfigProperties, uniParcHeartbeatProducer);
    }
}
