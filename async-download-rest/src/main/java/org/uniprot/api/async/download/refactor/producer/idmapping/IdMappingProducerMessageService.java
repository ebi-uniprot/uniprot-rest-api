package org.uniprot.api.async.download.refactor.producer.idmapping;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.MessagingService;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageService;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

@Component
@Slf4j
public class IdMappingProducerMessageService
        extends ProducerMessageService<IdMappingDownloadRequest, IdMappingDownloadJob> {
    private final IdMappingJobService jobService;

    protected IdMappingProducerMessageService(
            IdMappingJobService jobService,
            MessageConverter messageConverter,
            MessagingService messagingService,
            HashGenerator<IdMappingDownloadRequest> hashGenerator,
            AsyncDownloadFileHandler asyncDownloadFileHandler,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
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
    protected void createDownloadJob(String jobId, IdMappingDownloadRequest request) {
        IdMappingDownloadJob.IdMappingDownloadJobBuilder jobBuilder =
                IdMappingDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .fields(request.getFields())
                .format(request.getFormat())
                .created(now)
                .updated(now);
        this.jobService.save(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
