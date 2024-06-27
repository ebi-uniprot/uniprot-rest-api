package org.uniprot.api.async.download.messaging.consumer.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumerTest;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.UniParcRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.mq.uniparc.UniParcMessagingService;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@ExtendWith(MockitoExtension.class)
class UniParcMessageConsumerTest
        extends MessageConsumerTest<UniParcDownloadRequest, UniParcDownloadJob> {
    @Mock private UniParcMessagingService uniParcMessagingService;
    @Mock private UniParcRequestProcessor uniParcRequestProcessor;
    @Mock private UniParcFileHandler uniParcAsyncDownloadFileHandler;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private MessageConverter uniParcMessageConverter;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcDownloadJob uniParcDownloadJob;

    @BeforeEach
    void setUp() {
        messagingService = uniParcMessagingService;
        requestProcessor = uniParcRequestProcessor;
        fileHandler = uniParcAsyncDownloadFileHandler;
        jobService = uniParcJobService;
        messageConverter = uniParcMessageConverter;
        downloadJob = uniParcDownloadJob;
        downloadRequest = uniParcDownloadRequest;
        messageConsumer =
                new UniParcMessageConsumer(
                        uniParcMessagingService,
                        uniParcRequestProcessor,
                        uniParcAsyncDownloadFileHandler,
                        uniParcJobService,
                        uniParcMessageConverter);
        mockCommon();
    }
}
