package org.uniprot.api.async.download.refactor.producer.uniref;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.uniref.UniRefAsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.uniref.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UniRefProducerMessageService
        extends SolrProducerMessageService<UniRefDownloadRequest, UniRefDownloadJob> {
    private final UniRefJobService jobService;

    protected UniRefProducerMessageService(
            UniRefJobService jobService,
            MessageConverter messageConverter,
            UniRefMessagingService messagingService,
            HashGenerator<UniRefDownloadRequest> hashGenerator,
            UniRefAsyncDownloadFileHandler asyncDownloadFileHandler,
            UniRefAsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
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
    protected void createDownloadJob(String jobId, UniRefDownloadRequest request) {
        UniRefDownloadJob.UniRefDownloadJobBuilder jobBuilder = UniRefDownloadJob.builder();
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
