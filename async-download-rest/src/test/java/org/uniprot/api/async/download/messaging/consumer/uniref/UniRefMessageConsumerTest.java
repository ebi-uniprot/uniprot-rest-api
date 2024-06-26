package org.uniprot.api.async.download.messaging.consumer.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumerTest;
import org.uniprot.api.async.download.messaging.consumer.processor.uniref.UniRefRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.mq.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefMessageConsumerTest
        extends MessageConsumerTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock private UniRefMessagingService uniRefMessagingService;
    @Mock private UniRefRequestProcessor uniRefRequestProcessor;
    @Mock private UniRefFileHandler uniRefAsyncDownloadFileHandler;
    @Mock private UniRefJobService uniRefJobService;
    @Mock private MessageConverter uniRefMessageConverter;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefDownloadJob uniRefDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = uniRefMessagingService;
        requestProcessor = uniRefRequestProcessor;
        fileHandler = uniRefAsyncDownloadFileHandler;
        jobService = uniRefJobService;
        messageConverter = uniRefMessageConverter;
        downloadJob = uniRefDownloadJob;
        downloadRequest = uniRefDownloadRequest;
        messageConsumer =
                new UniRefMessageConsumer(
                        uniRefMessagingService,
                        uniRefRequestProcessor,
                        uniRefAsyncDownloadFileHandler,
                        uniRefJobService,
                        uniRefMessageConverter);
        mockCommon();
    }
}
