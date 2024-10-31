package org.uniprot.api.async.download.messaging.service.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.messaging.service.RabbitMQMessagingService;

@Component
public class UniRefMessagingService extends RabbitMQMessagingService {
    public UniRefMessagingService(
            UniRefAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniRefRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
