package org.uniprot.api.async.download.refactor.messaging;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;

@Component
public class UniProtKBMessagingService extends MessagingService {
    public UniProtKBMessagingService(
            UniProtKBAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniProtKBRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
