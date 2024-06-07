package org.uniprot.api.async.download.mq.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;
import org.uniprot.api.async.download.mq.MessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class UniProtKBMessagingServiceTest extends MessagingServiceTest {
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
