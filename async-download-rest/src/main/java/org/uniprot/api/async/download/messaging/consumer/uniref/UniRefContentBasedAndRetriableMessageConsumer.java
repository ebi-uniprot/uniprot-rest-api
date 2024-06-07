package org.uniprot.api.async.download.messaging.consumer.uniref;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.uniref.UniRefRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.mq.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@Component
public class UniRefContentBasedAndRetriableMessageConsumer
        extends ContentBasedAndRetriableMessageConsumer<UniRefDownloadRequest, UniRefDownloadJob> {
    public UniRefContentBasedAndRetriableMessageConsumer(
            UniRefMessagingService messagingService,
            UniRefRequestProcessor requestProcessor,
            UniRefAsyncDownloadFileHandler asyncDownloadFileHandler,
            UniRefJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
