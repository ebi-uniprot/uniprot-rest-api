package org.uniprot.api.async.download.messaging.consumer.mapto;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.MapToRequestProcessor;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.mq.mapto.MapToMessagingService;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class MapToMessageConsumer extends MessageConsumer<MapToDownloadRequest, MapToDownloadJob> {
    public MapToMessageConsumer(
            MapToMessagingService messagingService,
            MapToRequestProcessor requestProcessor,
            MapToFileHandler asyncDownloadFileHandler,
            MapToJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
