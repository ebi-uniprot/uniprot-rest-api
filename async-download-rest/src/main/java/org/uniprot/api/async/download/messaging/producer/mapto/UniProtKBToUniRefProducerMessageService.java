package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.messaging.service.mapto.MapToMessagingService;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.rest.request.HashGenerator;

@Component
public class UniProtKBToUniRefProducerMessageService
        extends MapToProducerMessageService<UniProtKBToUniRefDownloadRequest> {
    public UniProtKBToUniRefProducerMessageService(
            MapToJobService jobService,
            MessageConverter messageConverter,
            MapToMessagingService messagingService,
            HashGenerator<UniProtKBToUniRefDownloadRequest> hashGenerator,
            MapToFileHandler asyncDownloadFileHandler,
            MapToJobSubmissionRules<UniProtKBToUniRefDownloadRequest>
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
