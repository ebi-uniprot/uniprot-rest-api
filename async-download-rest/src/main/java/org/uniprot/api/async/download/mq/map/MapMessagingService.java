package org.uniprot.api.async.download.mq.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.map.MapAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.map.MapRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingService;

@Component
public class MapMessagingService extends RabbitMQMessagingService {
    public MapMessagingService(
            MapAsyncDownloadQueueConfigProperties queueConfigProperties,
            MapRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
