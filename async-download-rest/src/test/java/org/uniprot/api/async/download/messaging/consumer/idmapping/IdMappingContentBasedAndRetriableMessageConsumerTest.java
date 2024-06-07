package org.uniprot.api.async.download.messaging.consumer.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping.IdMappingRequestProcessor;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumerTest;
import org.uniprot.api.async.download.mq.idmapping.IdMappingMessagingService;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@ExtendWith(MockitoExtension.class)
class IdMappingContentBasedAndRetriableMessageConsumerTest extends ContentBasedAndRetriableMessageConsumerTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    @Mock private IdMappingMessagingService idMappingMessagingService;
    @Mock private IdMappingRequestProcessor idMappingRequestProcessor;
    @Mock private IdMappingAsyncDownloadFileHandler idMappingAsyncDownloadFileHandler;
    @Mock private IdMappingJobService idMappingJobService;
    @Mock private MessageConverter idMappingMessageConverter;
    @Mock
    private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock
    private IdMappingDownloadJob idMappingDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = idMappingMessagingService;
        requestProcessor = idMappingRequestProcessor;
        asyncDownloadFileHandler = idMappingAsyncDownloadFileHandler;
        jobService = idMappingJobService;
        messageConverter = idMappingMessageConverter;
        downloadJob = idMappingDownloadJob;
        downloadRequest = idMappingDownloadRequest;
        messageConsumer = new IdMappingContentBasedAndRetriableMessageConsumer(idMappingMessagingService, idMappingRequestProcessor, idMappingAsyncDownloadFileHandler, idMappingJobService, idMappingMessageConverter);
        mockCommon();
    }
}
