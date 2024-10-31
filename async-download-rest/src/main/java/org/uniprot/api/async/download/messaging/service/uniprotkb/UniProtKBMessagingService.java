package org.uniprot.api.async.download.messaging.service.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;
import org.uniprot.api.async.download.messaging.service.RabbitMQMessagingService;

@Component
public class UniProtKBMessagingService extends RabbitMQMessagingService {
    public UniProtKBMessagingService(
            UniProtKBAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniProtKBRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
