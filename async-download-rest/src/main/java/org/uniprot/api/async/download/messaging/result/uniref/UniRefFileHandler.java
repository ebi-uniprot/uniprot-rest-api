package org.uniprot.api.async.download.messaging.result.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;

@Component
public class UniRefFileHandler extends FileHandler {
    public UniRefFileHandler(
            UniRefDownloadConfigProperties uniRefDownloadConfigProperties,
            UniRefHeartbeatProducer uniRefHeartbeatProducer) {
        super(uniRefDownloadConfigProperties, uniRefHeartbeatProducer);
    }

    @Override
    public boolean areAllFilesPresent(String jobId) {
        return super.areIdAndResultFilesPresent(jobId);
    }
}
