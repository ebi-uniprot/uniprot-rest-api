package org.uniprot.api.async.download.messaging.producer.map;

import org.mockito.Mock;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageService;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageServiceTest;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.mq.map.MapMessagingService;
import org.uniprot.api.async.download.service.map.MapJobService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class MapProducerMessageServiceTest<T extends MapDownloadRequest>
        extends ProducerMessageServiceTest<T, MapDownloadJob> {
    @Mock protected MapJobService mapJobService;
    @Mock protected MessageConverter mapMessageConverter;
    @Mock protected MapMessagingService mapMessagingService;
    @Mock protected MapFileHandler mapDownloadFileHandler;

    void init() {
        this.jobService = mapJobService;
        this.messageConverter = mapMessageConverter;
        this.messagingService = mapMessagingService;
        this.fileHandler = mapDownloadFileHandler;
    }

    @Override
    protected void verifyPreprocess(T downloadRequest) {
        verify(downloadRequest).setLargeSolrStreamRestricted(false);
    }
}
