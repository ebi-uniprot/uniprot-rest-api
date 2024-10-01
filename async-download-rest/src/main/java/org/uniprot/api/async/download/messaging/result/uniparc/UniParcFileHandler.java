package org.uniprot.api.async.download.messaging.result.uniparc;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class UniParcFileHandler extends FileHandler {
    public UniParcFileHandler(
            UniParcDownloadConfigProperties uniParcDownloadConfigProperties,
            UniParcHeartbeatProducer uniParcHeartbeatProducer) {
        super(uniParcDownloadConfigProperties, uniParcHeartbeatProducer);
    }

    @Override
    public boolean areAllFilesPresent(String jobId) {
        return super.areIdAndResultFilesPresent(jobId);
    }
}
