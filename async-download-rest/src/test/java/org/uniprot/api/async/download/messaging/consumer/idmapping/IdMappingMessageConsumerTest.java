package org.uniprot.api.async.download.messaging.consumer.idmapping;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumerTest;
import org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping.IdMappingRequestProcessor;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.mq.idmapping.IdMappingRabbitMQMessagingService;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@ExtendWith(MockitoExtension.class)
class IdMappingMessageConsumerTest
        extends MessageConsumerTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    @Mock private IdMappingRabbitMQMessagingService idMappingMessagingService;
    @Mock private IdMappingRequestProcessor idMappingRequestProcessor;
    @Mock private IdMappingFileHandler idMappingAsyncDownloadFileHandler;
    @Mock private IdMappingJobService idMappingJobService;
    @Mock private MessageConverter idMappingMessageConverter;
    @Mock private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock private IdMappingDownloadJob idMappingDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = idMappingMessagingService;
        requestProcessor = idMappingRequestProcessor;
        fileHandler = idMappingAsyncDownloadFileHandler;
        jobService = idMappingJobService;
        messageConverter = idMappingMessageConverter;
        downloadJob = idMappingDownloadJob;
        downloadRequest = idMappingDownloadRequest;
        messageConsumer =
                new IdMappingMessageConsumer(
                        idMappingMessagingService,
                        idMappingRequestProcessor,
                        idMappingAsyncDownloadFileHandler,
                        idMappingJobService,
                        idMappingMessageConverter);
        mockCommon();
    }

    @Override
    protected void mockFileExistence() {
        when(fileHandler.isResultFilePresent(ID)).thenReturn(true);
    }
}
