package org.uniprot.api.async.download.messaging.producer.uniparc;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.mq.uniparc.UniParcMessagingService;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UniParcProducerMessageService
        extends SolrProducerMessageService<UniParcDownloadRequest, UniParcDownloadJob> {
    private final UniParcJobService jobService;

    protected UniParcProducerMessageService(
            UniParcJobService jobService,
            MessageConverter messageConverter,
            UniParcMessagingService messagingService,
            HashGenerator<UniParcDownloadRequest> hashGenerator,
            UniParcFileHandler asyncDownloadFileHandler,
            UniParcJobSubmissionRules asyncDownloadSubmissionRules) {
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
    protected void createDownloadJob(String jobId, UniParcDownloadRequest request) {
        UniParcDownloadJob.UniParcDownloadJobBuilder jobBuilder = UniParcDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(request.getQuery())
                .fields(request.getFields())
                .sort(request.getSort())
                .format(request.getFormat())
                .created(now)
                .updated(now);
        jobService.create(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
