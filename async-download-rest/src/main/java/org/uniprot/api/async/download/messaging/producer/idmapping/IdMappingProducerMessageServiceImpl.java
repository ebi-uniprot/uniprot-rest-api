package org.uniprot.api.async.download.messaging.producer.idmapping;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.JobSubmitFeedback;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IdMappingProducerMessageServiceImpl implements IdMappingProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final IdMappingDownloadJobRepository jobRepository;
    private final HashGenerator<IdMappingDownloadRequest> idMappingHashGenerator;
    private final AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    private static final String JOB_ID = "jobId";

    public IdMappingProducerMessageServiceImpl(
            MessageConverter converter,
            @Qualifier("idMappingRabbitTemplate") RabbitTemplate rabbitTemplate,
            IdMappingDownloadJobRepository downloadJobRepository,
            HashGenerator<IdMappingDownloadRequest> asyncIdMappingHashGenerator,
            AsyncDownloadSubmissionRules idMappingAsyncDownloadSubmissionRules,
            AsyncDownloadFileHandler idMappingAsyncDownloadFileHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        this.idMappingHashGenerator = asyncIdMappingHashGenerator;
        this.asyncDownloadSubmissionRules = idMappingAsyncDownloadSubmissionRules;
        this.asyncDownloadFileHandler = idMappingAsyncDownloadFileHandler;
    }

    @Override
    public String sendMessage(IdMappingDownloadRequest downloadRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = idMappingHashGenerator.generateHash(downloadRequest);
        messageHeader.setHeader(JOB_ID, jobId);

        if (Objects.isNull(downloadRequest.getFormat())) {
            downloadRequest.setFormat(MediaType.APPLICATION_JSON_VALUE);
        }
        boolean force = downloadRequest.isForce();
        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(jobId, force);

        if (jobSubmitFeedback.isAllowed()) {
            if (force) {
                cleanBeforeResubmission(jobId);
                log.info("Message with jobId {} is ready to resubmit", jobId);
            }
            doSendMessage(downloadRequest, messageHeader, jobId);
            log.info("Message with jobId {} ready to be processed", jobId);
        } else {
            alreadyProcessed(jobId);
            throw new IllegalDownloadJobSubmissionException(jobId, jobSubmitFeedback.getMessage());
        }
        return jobId;
    }

    private void cleanBeforeResubmission(String jobId) {
        jobRepository.deleteById(jobId);
        asyncDownloadFileHandler.deleteAllFiles(jobId);
    }

    @Override
    public void alreadyProcessed(String jobId) {
        log.info("Job is either being processed or already processed {}", jobId);
    }

    private void doSendMessage(
            IdMappingDownloadRequest downloadRequest,
            MessageProperties messageHeader,
            String asyncDownloadJobId) {
        Message message = converter.toMessage(downloadRequest, messageHeader);
        // write to redis and put on queue
        createDownloadJob(downloadRequest, asyncDownloadJobId);
        log.info("Message with jobId {} created in redis", asyncDownloadJobId);
        try {
            this.rabbitTemplate.send(message);
            log.info("Message with jobId sent to download queue {}", asyncDownloadJobId);
        } catch (AmqpException amqpException) {
            log.error("Unable to send message to the queue with exception", amqpException);
            this.jobRepository.deleteById(asyncDownloadJobId);
            throw amqpException;
        }
    }

    private void createDownloadJob(
            IdMappingDownloadRequest downloadRequest, String asyncDownloadJobId) {
        IdMappingDownloadJob.IdMappingDownloadJobBuilder jobBuilder =
                IdMappingDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(asyncDownloadJobId).status(JobStatus.NEW);
        jobBuilder
                .fields(downloadRequest.getFields())
                .format(downloadRequest.getFormat())
                .created(now)
                .updated(now);
        this.jobRepository.save(jobBuilder.build());
    }
}
