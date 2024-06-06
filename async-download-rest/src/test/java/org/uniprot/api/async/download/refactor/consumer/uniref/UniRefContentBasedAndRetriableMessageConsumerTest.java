package org.uniprot.api.async.download.refactor.consumer.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumerTest;
import org.uniprot.api.async.download.refactor.consumer.processor.uniref.UniRefRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefContentBasedAndRetriableMessageConsumerTest extends ContentBasedAndRetriableMessageConsumerTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock private UniRefMessagingService uniRefMessagingService;
    @Mock private UniRefRequestProcessor uniRefRequestProcessor;
    @Mock private UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler;
    @Mock private UniRefJobService uniRefJobService;
    @Mock private MessageConverter uniRefMessageConverter;
    @Mock
    private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock
    private UniRefDownloadJob uniRefDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = uniRefMessagingService;
        requestProcessor = uniRefRequestProcessor;
        asyncDownloadFileHandler = uniRefAsyncDownloadFileHandler;
        jobService = uniRefJobService;
        messageConverter = uniRefMessageConverter;
        downloadJob = uniRefDownloadJob;
        downloadRequest = uniRefDownloadRequest;
        messageConsumer = new UniRefContentBasedAndRetriableMessageConsumer(uniRefMessagingService, uniRefRequestProcessor, uniRefAsyncDownloadFileHandler, uniRefJobService, uniRefMessageConverter);
        mockCommon();
    }
}
