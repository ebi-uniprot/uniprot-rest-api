package org.uniprot.api.async.download.refactor.consumer.listener.uniref;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.listener.ContentBasedAndRetriableMessageListener;
import org.uniprot.api.async.download.refactor.messaging.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniref.UniRefIdResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@Component
public class UniRefContentBasedAndRetriableMessageListener extends ContentBasedAndRetriableMessageListener<UniRefDownloadRequest, UniRefDownloadJob> {
    public UniRefContentBasedAndRetriableMessageListener(UniRefMessagingService messagingService, UniRefIdResultRequestProcessor requestProcessor, UniRefAsyncDownloadFileHandler asyncDownloadFileHandler, UniRefJobService jobService, MessageConverter messageConverter) {
        super(messagingService, requestProcessor, asyncDownloadFileHandler, jobService, messageConverter);
    }
}
