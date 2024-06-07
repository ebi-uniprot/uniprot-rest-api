package org.uniprot.api.async.download.messaging.consumer.uniprotkb;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.UniProtKBRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.mq.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBContentBasedAndRetriableMessageConsumer
        extends ContentBasedAndRetriableMessageConsumer<
                        UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public UniProtKBContentBasedAndRetriableMessageConsumer(
            UniProtKBMessagingService messagingService,
            UniProtKBRequestProcessor requestProcessor,
            UniProtKBAsyncDownloadFileHandler asyncDownloadFileHandler,
            UniProtKBJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
