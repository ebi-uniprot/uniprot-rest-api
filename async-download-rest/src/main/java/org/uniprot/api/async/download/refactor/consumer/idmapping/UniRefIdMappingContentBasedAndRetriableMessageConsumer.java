package org.uniprot.api.async.download.refactor.consumer.idmapping;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping.UniParcIdMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping.UniRefIdMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.uniref.UniRefRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.idmapping.IdMappingMessagingService;
import org.uniprot.api.async.download.refactor.messaging.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@Component
public class UniRefIdMappingContentBasedAndRetriableMessageConsumer
        extends ContentBasedAndRetriableMessageConsumer<IdMappingDownloadRequest, IdMappingDownloadJob> {
    public UniRefIdMappingContentBasedAndRetriableMessageConsumer(
            IdMappingMessagingService messagingService,
            UniRefIdMappingResultRequestProcessor requestProcessor,
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
