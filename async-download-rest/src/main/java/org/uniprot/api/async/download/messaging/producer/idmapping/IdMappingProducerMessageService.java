package org.uniprot.api.async.download.messaging.producer.idmapping;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageService;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.mq.idmapping.IdMappingRabbitMQMessagingService;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IdMappingProducerMessageService
        extends ProducerMessageService<IdMappingDownloadRequest, IdMappingDownloadJob> {
    private final IdMappingJobService jobService;

    protected IdMappingProducerMessageService(
            IdMappingJobService jobService,
            MessageConverter messageConverter,
            IdMappingRabbitMQMessagingService messagingService,
            HashGenerator<IdMappingDownloadRequest> hashGenerator,
            IdMappingFileHandler asyncDownloadFileHandler,
            IdMappingJobSubmissionRules asyncDownloadSubmissionRules) {
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
        jobService.create(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
