package org.uniprot.api.async.download.refactor.consumer.uniprotkb;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.consumer.processor.uniprotkb.UniProtKBRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBContentBasedAndRetriableMessageConsumer
        extends ContentBasedAndRetriableMessageConsumer<
                UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public UniProtKBContentBasedAndRetriableMessageConsumer(
            UniProtKBMessagingService messagingService,
            UniProtKBRequestProcessor requestProcessor,
            UniRefAsyncDownloadFileHandler asyncDownloadFileHandler,
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
