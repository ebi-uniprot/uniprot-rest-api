package org.uniprot.api.async.download.messaging.producer.mapto;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.messaging.service.mapto.MapToMessagingService;
import org.uniprot.api.async.download.model.request.mapto.UniParcToUniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.rest.request.HashGenerator;

@Component
public class UniParcToUniProtKBProducerMessageService extends MapToProducerMessageService<UniParcToUniProtKBMapDownloadRequest>{
    public UniParcToUniProtKBProducerMessageService(MapToJobService jobService, MessageConverter messageConverter,
                                                    MapToMessagingService messagingService,
                                                    HashGenerator<UniParcToUniProtKBMapDownloadRequest> hashGenerator,
                                                    MapToFileHandler asyncDownloadFileHandler,
                                                    MapToJobSubmissionRules<UniParcToUniProtKBMapDownloadRequest> asyncDownloadSubmissionRules) {
        super(jobService, messageConverter, messagingService, hashGenerator,
                asyncDownloadFileHandler, asyncDownloadSubmissionRules);
    }
}
