package org.uniprot.api.async.download.messaging.service.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;
import org.uniprot.api.async.download.messaging.service.RabbitMQMessagingService;
// todo move and merge pkgs

@Component
public class IdMappingRabbitMQMessagingService extends RabbitMQMessagingService {
    public IdMappingRabbitMQMessagingService(
            IdMappingAsyncDownloadQueueConfigProperties queueConfigProperties,
            IdMappingRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
