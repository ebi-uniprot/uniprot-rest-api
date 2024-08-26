package org.uniprot.api.async.download.messaging.consumer.map;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.map.MapRequestProcessor;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.mq.map.MapMessagingService;
import org.uniprot.api.async.download.service.map.MapJobService;

@Component
public class MapMessageConsumer extends MessageConsumer<MapDownloadRequest, MapDownloadJob> {
    public MapMessageConsumer(
            MapMessagingService messagingService,
            MapRequestProcessor requestProcessor,
            MapFileHandler asyncDownloadFileHandler,
            MapJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                asyncDownloadFileHandler,
                jobService,
                messageConverter);
    }
}
