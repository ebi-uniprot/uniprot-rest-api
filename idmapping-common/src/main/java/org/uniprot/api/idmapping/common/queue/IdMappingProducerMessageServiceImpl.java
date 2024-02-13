package org.uniprot.api.idmapping.common.queue;

import java.time.LocalDateTime;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.request.IdMappingDownloadRequest;
import org.uniprot.api.rest.download.file.AsyncDownloadFileHandler;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.AsyncDownloadSubmissionRules;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.job.JobSubmitFeedback;
import org.uniprot.api.rest.request.HashGenerator;

@Service
@Slf4j
@Profile({"asyncDownload"})
public class IdMappingProducerMessageServiceImpl implements IdMappingProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;
    private final HashGenerator<IdMappingDownloadRequest> idMappingHashGenerator;
    private final AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    private static final String JOB_ID = "jobId";

    public IdMappingProducerMessageServiceImpl(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<IdMappingDownloadRequest> idMappingHashGenerator,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules,
            AsyncDownloadFileHandler asyncDownloadFileHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        this.idMappingHashGenerator = idMappingHashGenerator;
        this.asyncDownloadSubmissionRules = asyncDownloadSubmissionRules;
        this.asyncDownloadFileHandler = asyncDownloadFileHandler;
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
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
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
