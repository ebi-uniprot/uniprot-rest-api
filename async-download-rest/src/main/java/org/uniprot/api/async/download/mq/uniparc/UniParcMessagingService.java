package org.uniprot.api.async.download.mq.uniparc;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingService;

@Component
public class UniParcMessagingService extends RabbitMQMessagingService {
    public UniParcMessagingService(
            UniParcAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniParcRabbitTemplate rabbitTemplate) {
        super(queueConfigProperties, rabbitTemplate);
    }
}
