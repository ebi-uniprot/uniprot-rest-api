package org.uniprot.api.async.download.queue.common;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDateTime;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.model.DownloadJob;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.output.job.JobSubmitFeedback;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Slf4j
public abstract class RabbitProducerMessageService implements ProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;
    private final HashGenerator<DownloadRequest> hashGenerator;
    private final AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    public static final String JOB_ID = "jobId";

    public RabbitProducerMessageService(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<DownloadRequest> hashGenerator,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules,
            AsyncDownloadFileHandler asyncDownloadFileHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        this.hashGenerator = hashGenerator;
        this.asyncDownloadSubmissionRules = asyncDownloadSubmissionRules;
        this.asyncDownloadFileHandler = asyncDownloadFileHandler;
    }

    @Override
    public String sendMessage(DownloadRequest downloadRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = this.hashGenerator.generateHash(downloadRequest);
        messageHeader.setHeader(JOB_ID, jobId);

        if (Objects.isNull(downloadRequest.getFormat())) {
            downloadRequest.setFormat(APPLICATION_JSON_VALUE);
        }

        downloadRequest.setLargeSolrStreamRestricted(false);
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
        log.info("Job is already processed {}", jobId);
    }

    private void doSendMessage(
            DownloadRequest downloadRequest, MessageProperties messageHeader, String jobId) {
        Message message = converter.toMessage(downloadRequest, messageHeader);
        // write to redis and put on queue
        createDownloadJob(jobId, downloadRequest);
        log.info("Message with jobId {} created in redis", jobId);
        try {
            this.rabbitTemplate.send(message);
            log.info("Message with jobId sent to download queue {}", jobId);
        } catch (AmqpException amqpException) {
            log.error("Unable to send message to the queue with exception {}", amqpException);
            this.jobRepository.deleteById(jobId);
            throw amqpException;
        }
    }

    private void createDownloadJob(String jobId, DownloadRequest downloadRequest) {
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(downloadRequest.getQuery())
                .fields(downloadRequest.getFields())
                .sort(downloadRequest.getSort())
                .format(downloadRequest.getFormat())
                .created(now)
                .updated(now);
        this.jobRepository.save(jobBuilder.build());
    }
}
