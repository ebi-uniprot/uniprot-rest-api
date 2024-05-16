package org.uniprot.api.async.download.refactor.consumer.uniref;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.consumer.processor.uniref.UniRefRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

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
