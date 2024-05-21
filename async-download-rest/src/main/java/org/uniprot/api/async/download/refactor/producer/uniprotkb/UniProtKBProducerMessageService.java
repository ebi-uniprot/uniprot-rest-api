package org.uniprot.api.async.download.refactor.producer.uniprotkb;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.uniprotkb.UniProtKBAsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UniProtKBProducerMessageService
        extends SolrProducerMessageService<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private final UniProtKBJobService jobService;

    protected UniProtKBProducerMessageService(
            UniProtKBJobService jobService,
            MessageConverter messageConverter,
            UniProtKBMessagingService messagingService,
            HashGenerator<UniProtKBDownloadRequest> uniProtKBDownloadHashGenerator,
            UniProtKBAsyncDownloadFileHandler asyncDownloadFileHandler,
            UniProtKBAsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                uniProtKBDownloadHashGenerator,
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
