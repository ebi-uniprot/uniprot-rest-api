package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.messaging.service.mapto.MapToMessagingService;
import org.uniprot.api.async.download.model.request.mapto.UniRefToUniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.rest.request.HashGenerator;

@Component
public class UniRefToUniProtKBProducerMessageService
        extends MapToProducerMessageService<UniRefToUniProtKBDownloadRequest> {
    public UniRefToUniProtKBProducerMessageService(
            MapToJobService jobService,
            MessageConverter messageConverter,
            MapToMessagingService messagingService,
            HashGenerator<UniRefToUniProtKBDownloadRequest> hashGenerator,
            MapToFileHandler asyncDownloadFileHandler,
            UniRefToUniProtKBJobSubmissionRules
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
