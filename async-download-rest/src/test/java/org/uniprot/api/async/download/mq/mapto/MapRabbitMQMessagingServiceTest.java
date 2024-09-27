package org.uniprot.api.async.download.mq.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.mapto.MapToAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.mapto.MapToRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class MapRabbitMQMessagingServiceTest extends RabbitMQMessagingServiceTest {
    @Mock private MapToAsyncDownloadQueueConfigProperties mapToAsyncDownloadQueueConfigProperties;

    @Mock private MapToRabbitTemplate mapToRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = mapToAsyncDownloadQueueConfigProperties;
        rabbitTemplate = mapToRabbitTemplate;
        messagingService =
                new MapToMessagingService(mapToAsyncDownloadQueueConfigProperties, mapToRabbitTemplate);
    }
}
