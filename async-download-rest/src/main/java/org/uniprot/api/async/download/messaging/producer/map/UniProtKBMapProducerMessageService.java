package org.uniprot.api.async.download.messaging.producer.map;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.async.download.mq.map.MapMessagingService;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.rest.request.HashGenerator;

@Component
public class UniProtKBMapProducerMessageService
        extends MapProducerMessageService<UniProtKBToUniRefMapDownloadRequest> {
    public UniProtKBMapProducerMessageService(
            MapJobService jobService,
            MessageConverter messageConverter,
            MapMessagingService messagingService,
            HashGenerator<UniProtKBToUniRefMapDownloadRequest> hashGenerator,
            MapFileHandler asyncDownloadFileHandler,
            MapJobSubmissionRules<UniProtKBToUniRefMapDownloadRequest>
                    asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                hashGenerator,
                asyncDownloadFileHandler,
                asyncDownloadSubmissionRules);
    }
}
