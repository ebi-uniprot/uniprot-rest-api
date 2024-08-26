package org.uniprot.api.async.download.messaging.consumer.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumerTest;
import org.uniprot.api.async.download.messaging.consumer.processor.map.MapRequestProcessor;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.mq.map.MapMessagingService;
import org.uniprot.api.async.download.service.map.MapJobService;

@ExtendWith(MockitoExtension.class)
class MapMessageConsumerTest
        extends MessageConsumerTest<MapDownloadRequest, MapDownloadJob> {
    @Mock
    private MapMessagingService mapMessagingService;
    @Mock
    private MapFileHandler mapFileHandler;
    @Mock
    private MapJobService mapJobService;
    @Mock
    private MessageConverter mapMessageConverter;
    @Mock
    private MapDownloadJob mapDownloadJob;
    @Mock
    private MapRequestProcessor mapRequestProcessor;
    @Mock
    private MapDownloadRequest mapDownloadRequest;


    @BeforeEach
    void setUp() {
        messagingService = mapMessagingService;
        fileHandler = mapFileHandler;
        jobService = mapJobService;
        messageConverter = mapMessageConverter;
        downloadJob = mapDownloadJob;
        requestProcessor = mapRequestProcessor;
        downloadRequest = mapDownloadRequest;
        messageConsumer =
                new MapMessageConsumer(
                        mapMessagingService,
                        mapRequestProcessor,
                        mapFileHandler,
                        mapJobService,
                        mapMessageConverter);
        mockCommon();
    }
}
