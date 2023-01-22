package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.MessageProperties;
import org.uniprot.api.rest.request.StreamRequest;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public interface ProducerMessageService {
    void sendMessage(StreamRequest streamRequest, MessageProperties messageHeader);

    void logAlreadyProcessed(String jobId);
}
