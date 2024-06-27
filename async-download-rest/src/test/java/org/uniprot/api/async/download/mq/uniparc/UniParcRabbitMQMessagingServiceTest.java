package org.uniprot.api.async.download.mq.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcRabbitTemplate;
import org.uniprot.api.async.download.mq.RabbitMQMessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class UniParcRabbitMQMessagingServiceTest extends RabbitMQMessagingServiceTest {
    @Mock
    private UniParcAsyncDownloadQueueConfigProperties uniParcAsyncDownloadQueueConfigProperties;

    @Mock private UniParcRabbitTemplate uniParcRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = uniParcAsyncDownloadQueueConfigProperties;
        rabbitTemplate = uniParcRabbitTemplate;
        messagingService =
                new UniParcMessagingService(
                        uniParcAsyncDownloadQueueConfigProperties, uniParcRabbitTemplate);
    }
}
