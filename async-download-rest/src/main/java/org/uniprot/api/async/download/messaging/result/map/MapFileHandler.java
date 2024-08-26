package org.uniprot.api.async.download.messaging.result.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class MapFileHandler extends FileHandler {
    public MapFileHandler(
            MapDownloadConfigProperties uniParcDownloadConfigProperties,
            MapHeartbeatProducer uniParcHeartbeatProducer) {
        super(uniParcDownloadConfigProperties, uniParcHeartbeatProducer);
    }
}
