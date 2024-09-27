package org.uniprot.api.async.download.messaging.producer.mapto;

import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageServiceTest;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.mq.mapto.MapToMessagingService;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

public abstract class MapToProducerMessageServiceTest<T extends MapToDownloadRequest>
        extends ProducerMessageServiceTest<T, MapToDownloadJob> {
    @Mock protected MapToJobService mapToJobService;
    @Mock protected MessageConverter mapMessageConverter;
    @Mock protected MapToMessagingService mapToMessagingService;
    @Mock protected MapToFileHandler mapDownloadFileHandler;

    void init() {
        this.jobService = mapToJobService;
        this.messageConverter = mapMessageConverter;
        this.messagingService = mapToMessagingService;
        this.fileHandler = mapDownloadFileHandler;
    }

    @Override
    protected void verifyPreprocess(T downloadRequest) {
        verify(downloadRequest).setLargeSolrStreamRestricted(false);
    }
}
