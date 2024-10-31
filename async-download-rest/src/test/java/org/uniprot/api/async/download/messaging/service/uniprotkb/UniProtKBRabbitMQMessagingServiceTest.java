package org.uniprot.api.async.download.messaging.service.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;
import org.uniprot.api.async.download.messaging.service.RabbitMQMessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class UniProtKBRabbitMQMessagingServiceTest extends RabbitMQMessagingServiceTest {
    @Mock
    private UniProtKBAsyncDownloadQueueConfigProperties uniProtKBAsyncDownloadQueueConfigProperties;

    @Mock private UniProtKBRabbitTemplate uniProtKBRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = uniProtKBAsyncDownloadQueueConfigProperties;
        rabbitTemplate = uniProtKBRabbitTemplate;
        messagingService =
                new UniProtKBMessagingService(
                        uniProtKBAsyncDownloadQueueConfigProperties, uniProtKBRabbitTemplate);
    }
}
