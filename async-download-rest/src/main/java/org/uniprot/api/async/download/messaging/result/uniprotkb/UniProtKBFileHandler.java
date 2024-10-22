package org.uniprot.api.async.download.messaging.result.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class UniProtKBFileHandler extends FileHandler {
    public UniProtKBFileHandler(
            UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties,
            UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer) {
        super(uniProtKBDownloadConfigProperties, uniProtKBHeartbeatProducer);
    }

    @Override
    public boolean areAllFilesPresent(String jobId) {
        return super.areIdAndResultFilesPresent(jobId);
    }
}
