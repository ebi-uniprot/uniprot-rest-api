package org.uniprot.api.async.download.messaging.consumer.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumerTest;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.MapToRequestProcessor;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.mq.mapto.MapToMessagingService;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@ExtendWith(MockitoExtension.class)
class MapToMessageConsumerTest extends MessageConsumerTest<MapToDownloadRequest, MapToDownloadJob> {
    @Mock private MapToMessagingService mapToMessagingService;
    @Mock private MapToFileHandler mapToFileHandler;
    @Mock private MapToJobService mapToJobService;
    @Mock private MessageConverter mapMessageConverter;
    @Mock private MapToDownloadJob mapToDownloadJob;
    @Mock private MapToRequestProcessor mapToRequestProcessor;
    @Mock private MapToDownloadRequest mapToDownloadRequest;

    @BeforeEach
    void setUp() {
        messagingService = mapToMessagingService;
        fileHandler = mapToFileHandler;
        jobService = mapToJobService;
        messageConverter = mapMessageConverter;
        downloadJob = mapToDownloadJob;
        requestProcessor = mapToRequestProcessor;
        downloadRequest = mapToDownloadRequest;
        messageConsumer =
                new MapToMessageConsumer(
                        mapToMessagingService,
                        mapToRequestProcessor,
                        mapToFileHandler,
                        mapToJobService,
                        mapMessageConverter);
        mockCommon();
    }
}
