package org.uniprot.api.async.download.refactor.consumer.listener.uniprotkb;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.listener.ContentBasedAndRetriableMessageListener;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniprotkb.UniProtKBRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBContentBasedAndRetriableMessageListener extends ContentBasedAndRetriableMessageListener<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public UniProtKBContentBasedAndRetriableMessageListener(UniProtKBMessagingService messagingService, UniProtKBRequestProcessor requestProcessor, UniRefAsyncDownloadFileHandler asyncDownloadFileHandler, UniProtKBJobService jobService, MessageConverter messageConverter) {
        super(messagingService, requestProcessor, asyncDownloadFileHandler, jobService, messageConverter);
    }
}
