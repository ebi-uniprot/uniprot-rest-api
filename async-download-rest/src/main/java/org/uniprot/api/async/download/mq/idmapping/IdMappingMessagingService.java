package org.uniprot.api.async.download.mq.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;
import org.uniprot.api.async.download.mq.MessagingService;

@Component
public class IdMappingMessagingService extends MessagingService {
    public IdMappingMessagingService(
            IdMappingAsyncDownloadQueueConfigProperties queueConfigProperties,
            IdMappingRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
