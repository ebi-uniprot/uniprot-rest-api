package org.uniprot.api.async.download.mq.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.mapto.MapToAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.mapto.MapToRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingService;

@Component
public class MapToMessagingService extends RabbitMQMessagingService {
    public MapToMessagingService(
            MapToAsyncDownloadQueueConfigProperties queueConfigProperties,
            MapToRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
