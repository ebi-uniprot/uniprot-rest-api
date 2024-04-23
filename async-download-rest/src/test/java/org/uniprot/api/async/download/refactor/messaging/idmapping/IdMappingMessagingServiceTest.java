package org.uniprot.api.async.download.refactor.messaging.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;
import org.uniprot.api.async.download.refactor.messaging.MessagingServiceTest;

@ExtendWith(MockitoExtension.class)
class IdMappingMessagingServiceTest extends MessagingServiceTest {
    @Mock
    private IdMappingAsyncDownloadQueueConfigProperties idMappingAsyncDownloadQueueConfigProperties;
    @Mock
    private IdMappingRabbitTemplate idMappingRabbitTemplate;

    @BeforeEach
    void setUp() {
        queueConfigProperties = idMappingAsyncDownloadQueueConfigProperties;
        rabbitTemplate = idMappingRabbitTemplate;
        messagingService = new IdMappingMessagingService(idMappingAsyncDownloadQueueConfigProperties, idMappingRabbitTemplate);
    }
}
