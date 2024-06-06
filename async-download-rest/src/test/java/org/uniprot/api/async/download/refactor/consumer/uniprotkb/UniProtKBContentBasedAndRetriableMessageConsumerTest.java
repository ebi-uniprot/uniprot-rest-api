package org.uniprot.api.async.download.refactor.consumer.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumerTest;
import org.uniprot.api.async.download.refactor.consumer.processor.uniprotkb.UniProtKBRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class UniProtKBContentBasedAndRetriableMessageConsumerTest extends ContentBasedAndRetriableMessageConsumerTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    @Mock private UniProtKBMessagingService uniProtKBMessagingService;
    @Mock private UniProtKBRequestProcessor uniProtKBRequestProcessor;
    @Mock private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private MessageConverter uniProtKBMessageConverter;
    @Mock
    private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock
    private UniProtKBDownloadJob uniProtKBDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = uniProtKBMessagingService;
        requestProcessor = uniProtKBRequestProcessor;
        asyncDownloadFileHandler = uniProtKBAsyncDownloadFileHandler;
        jobService = uniProtKBJobService;
        messageConverter = uniProtKBMessageConverter;
        downloadJob = uniProtKBDownloadJob;
        downloadRequest = uniProtKBDownloadRequest;
        messageConsumer = new UniProtKBContentBasedAndRetriableMessageConsumer(uniProtKBMessagingService, uniProtKBRequestProcessor, uniProtKBAsyncDownloadFileHandler, uniProtKBJobService, uniProtKBMessageConverter);
        mockCommon();
    }
}
