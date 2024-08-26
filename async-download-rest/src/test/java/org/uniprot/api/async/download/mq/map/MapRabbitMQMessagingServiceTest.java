package org.uniprot.api.async.download.mq.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.map.MapAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.map.MapRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class MapRabbitMQMessagingServiceTest extends RabbitMQMessagingServiceTest {
    @Mock private MapAsyncDownloadQueueConfigProperties mapAsyncDownloadQueueConfigProperties;

    @Mock private MapRabbitTemplate mapRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = mapAsyncDownloadQueueConfigProperties;
        rabbitTemplate = mapRabbitTemplate;
        messagingService =
                new MapMessagingService(mapAsyncDownloadQueueConfigProperties, mapRabbitTemplate);
    }
}
