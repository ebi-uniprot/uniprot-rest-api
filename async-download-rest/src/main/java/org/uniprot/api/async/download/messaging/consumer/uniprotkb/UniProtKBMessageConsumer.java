package org.uniprot.api.async.download.messaging.consumer.uniprotkb;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.UniProtKBRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.mq.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBMessageConsumer
        extends MessageConsumer<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public UniProtKBMessageConsumer(
            UniProtKBMessagingService messagingService,
            UniProtKBRequestProcessor requestProcessor,
            UniProtKBFileHandler fileHandler,
            UniProtKBJobService jobService,
            MessageConverter messageConverter) {
        super(
                messagingService,
                requestProcessor,
                fileHandler,
                jobService,
                messageConverter);
    }
}
