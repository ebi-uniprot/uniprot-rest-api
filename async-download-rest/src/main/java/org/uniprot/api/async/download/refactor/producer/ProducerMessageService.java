package org.uniprot.api.async.download.refactor.producer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Objects;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.JobSubmitFeedback;
import org.uniprot.api.async.download.refactor.messaging.MessagingService;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ProducerMessageService<T extends DownloadRequest, R extends DownloadJob> {
    public static final String JOB_ID = "jobId";
    private final JobService<R> jobService;
    private final MessageConverter messageConverter;
    private final MessagingService messagingService;
    private final HashGenerator<T> hashGenerator;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    private final AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;

    protected ProducerMessageService(
            JobService<R> jobService,
            MessageConverter messageConverter,
            MessagingService messagingService,
            HashGenerator<T> hashGenerator,
            AsyncDownloadFileHandler asyncDownloadFileHandler,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules) {
        this.jobService = jobService;
        this.messageConverter = messageConverter;
        this.messagingService = messagingService;
        this.hashGenerator = hashGenerator;
        this.asyncDownloadFileHandler = asyncDownloadFileHandler;
        this.asyncDownloadSubmissionRules = asyncDownloadSubmissionRules;
    }

    public String sendMessage(T request) {
        preprocess(request);

        String jobId = this.hashGenerator.generateHash(request);
        boolean force = request.isForce();
        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(jobId, force);

        if (jobSubmitFeedback.isAllowed()) {
            cleanIfNecessary(jobId, force);
            createDownloadJob(jobId, request);
            sendMessage(jobId, request);
        } else {
            log.info("Job is either being processed or already processed {}", jobId);
            throw new IllegalDownloadJobSubmissionException(jobId, jobSubmitFeedback.getMessage());
        }

        return jobId;
    }

    protected void preprocess(T request) {
        if (Objects.isNull(request.getFormat())) {
            request.setFormat(APPLICATION_JSON_VALUE);
        }
    }

    private void cleanIfNecessary(String jobId, boolean force) {
        if (force) {
            jobService.delete(jobId);
            asyncDownloadFileHandler.deleteAllFiles(jobId);
            log.info("Message with jobId {} is ready to resubmit", jobId);
        }
    }

    protected abstract void createDownloadJob(String jobId, T request);

    private void sendMessage(String jobId, T request) {
        try {
            MessageProperties messageHeader = new MessageProperties();
            messageHeader.setHeader(JOB_ID, jobId);
            Message message = messageConverter.toMessage(request, messageHeader);
            log.info("Message with jobId {} ready to be processed", jobId);
            this.messagingService.send(message);
            log.info("Message with jobId sent to download queue {}", jobId);
        } catch (AmqpException amqpException) {
            log.error("Unable to send message to the queue with exception {}", amqpException);
            this.jobService.delete(jobId);
            throw amqpException;
        }
    }
}
