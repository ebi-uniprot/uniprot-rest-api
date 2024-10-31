package org.uniprot.api.async.download.messaging.service.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.mapto.MapToAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.mapto.MapToRabbitTemplate;
import org.uniprot.api.async.download.messaging.service.RabbitMQMessagingService;

@Component
public class MapToMessagingService extends RabbitMQMessagingService {
    public MapToMessagingService(
            MapToAsyncDownloadQueueConfigProperties queueConfigProperties,
            MapToRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
