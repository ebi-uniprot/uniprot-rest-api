package org.uniprot.api.async.download.messaging.consumer.uniparc;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.UniParcRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.mq.uniparc.UniParcMessagingService;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcMessageConsumer
        extends MessageConsumer<UniParcDownloadRequest, UniParcDownloadJob> {
    public UniParcMessageConsumer(
            UniParcMessagingService messagingService,
            UniParcRequestProcessor requestProcessor,
            UniParcFileHandler asyncDownloadFileHandler,
            UniParcJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
