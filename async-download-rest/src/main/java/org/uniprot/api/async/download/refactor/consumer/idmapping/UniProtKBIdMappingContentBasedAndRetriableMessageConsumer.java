package org.uniprot.api.async.download.refactor.consumer.idmapping;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping.UniProtKBMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.idmapping.IdMappingMessagingService;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;

@Component
public class UniProtKBIdMappingContentBasedAndRetriableMessageConsumer
        extends ContentBasedAndRetriableMessageConsumer<IdMappingDownloadRequest, IdMappingDownloadJob> {
    public UniProtKBIdMappingContentBasedAndRetriableMessageConsumer(
            IdMappingMessagingService messagingService,
            UniProtKBMappingResultRequestProcessor requestProcessor,
            IdMappingAsyncDownloadFileHandler asyncDownloadFileHandler,
            IdMappingJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
