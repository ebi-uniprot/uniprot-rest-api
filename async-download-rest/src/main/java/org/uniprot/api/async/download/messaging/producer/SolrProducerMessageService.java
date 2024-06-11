package org.uniprot.api.async.download.messaging.producer;

import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.async.download.mq.MessagingService;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.request.HashGenerator;

public abstract class SolrProducerMessageService<
                T extends SolrStreamDownloadRequest, R extends DownloadJob>
        extends ProducerMessageService<T, R> {

    protected SolrProducerMessageService(
            JobService<R> jobService,
            MessageConverter messageConverter,
            MessagingService messagingService,
            HashGenerator<T> hashGenerator,
            AsyncDownloadFileHandler asyncDownloadFileHandler,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                hashGenerator,
                asyncDownloadFileHandler,
                asyncDownloadSubmissionRules);
    }

    @Override
    protected void preprocess(T request) {
        super.preprocess(request);
        request.setLargeSolrStreamRestricted(false);
    }
}
