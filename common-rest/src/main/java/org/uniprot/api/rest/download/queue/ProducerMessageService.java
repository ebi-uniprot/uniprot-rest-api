package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.MessageProperties;
import org.uniprot.api.rest.request.DownloadRequest;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public interface ProducerMessageService {
    void sendMessage(DownloadRequest downloadRequest, MessageProperties messageHeader);

    void logAlreadyProcessed(String jobId);
}
