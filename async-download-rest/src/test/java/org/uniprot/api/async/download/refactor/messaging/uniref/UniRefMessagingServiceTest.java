package org.uniprot.api.async.download.refactor.messaging.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.refactor.messaging.MessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class UniRefMessagingServiceTest extends MessagingServiceTest {
    @Mock private UniRefAsyncDownloadQueueConfigProperties uniRefAsyncDownloadQueueConfigProperties;
    @Mock private UniRefRabbitTemplate uniRefRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = uniRefAsyncDownloadQueueConfigProperties;
        rabbitTemplate = uniRefRabbitTemplate;
        messagingService =
                new UniRefMessagingService(
                        uniRefAsyncDownloadQueueConfigProperties, uniRefRabbitTemplate);
    }
}
