package org.uniprot.api.async.download.mq.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class IdMappingRabbitMQRabbitMQMessagingServiceTest extends RabbitMQMessagingServiceTest {
    @Mock
    private IdMappingAsyncDownloadQueueConfigProperties idMappingAsyncDownloadQueueConfigProperties;

    @Mock private IdMappingRabbitTemplate idMappingRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = idMappingAsyncDownloadQueueConfigProperties;
        rabbitTemplate = idMappingRabbitTemplate;
        messagingService =
                new IdMappingRabbitMQMessagingService(
                        idMappingAsyncDownloadQueueConfigProperties, idMappingRabbitTemplate);
    }
}
