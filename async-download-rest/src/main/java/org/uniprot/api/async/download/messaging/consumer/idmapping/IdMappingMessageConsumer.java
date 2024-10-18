package org.uniprot.api.async.download.messaging.consumer.idmapping;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.idmapping.IdMappingRequestProcessor;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.mq.idmapping.IdMappingRabbitMQMessagingService;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@Component
public class IdMappingMessageConsumer
        extends MessageConsumer<IdMappingDownloadRequest, IdMappingDownloadJob> {

    public IdMappingMessageConsumer(
            IdMappingRabbitMQMessagingService messagingService,
            IdMappingRequestProcessor requestProcessor,
            IdMappingFileHandler fileHandler,
            IdMappingJobService jobService,
            MessageConverter messageConverter) {
        super(messagingService, requestProcessor, fileHandler, jobService, messageConverter);
    }
}
