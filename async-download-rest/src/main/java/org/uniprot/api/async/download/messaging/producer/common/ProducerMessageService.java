package org.uniprot.api.async.download.messaging.producer.common;

import org.uniprot.api.async.download.model.common.DownloadRequest;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public interface ProducerMessageService {
    String sendMessage(DownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
