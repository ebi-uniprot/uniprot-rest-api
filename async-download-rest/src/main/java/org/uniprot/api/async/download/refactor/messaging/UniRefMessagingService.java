package org.uniprot.api.async.download.refactor.messaging;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;

@Component
public class UniRefMessagingService extends MessagingService {
    public UniRefMessagingService(
            UniRefAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniRefRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
