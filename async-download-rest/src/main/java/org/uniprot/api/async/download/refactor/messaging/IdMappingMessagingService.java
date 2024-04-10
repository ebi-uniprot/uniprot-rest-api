package org.uniprot.api.async.download.refactor.messaging;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;

@Component
public class IdMappingMessagingService extends MessagingService {
    public IdMappingMessagingService(
            IdMappingAsyncDownloadQueueConfigProperties queueConfigProperties,
            IdMappingRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
