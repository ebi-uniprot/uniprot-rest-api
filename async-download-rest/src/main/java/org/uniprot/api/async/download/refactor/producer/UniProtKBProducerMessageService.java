package org.uniprot.api.async.download.refactor.producer;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.uniprotkb.UniProtKBAsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.UniProtKBJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

@Component
@Slf4j
public class UniProtKBProducerMessageService
        extends SolrProducerMessageService<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private final UniProtKBJobService jobService;

    protected UniProtKBProducerMessageService(
            UniProtKBJobService jobService,
            MessageConverter messageConverter,
            UniProtKBMessagingService messagingService,
            HashGenerator<UniProtKBDownloadRequest> hashGenerator,
            UniProtKBAsyncDownloadFileHandler asyncDownloadFileHandler,
            UniProtKBAsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                hashGenerator,
                asyncDownloadFileHandler,
                asyncDownloadSubmissionRules);
        this.jobService = jobService;
    }

    @Override
    protected void createDownloadJob(String jobId, UniProtKBDownloadRequest request) {
        UniProtKBDownloadJob.UniProtKBDownloadJobBuilder jobBuilder =
                UniProtKBDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(request.getQuery())
                .fields(request.getFields())
                .sort(request.getSort())
                .format(request.getFormat())
                .created(now)
                .updated(now);
        jobService.save(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
