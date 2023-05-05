package org.uniprot.api.rest.download.queue;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDateTime;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service
@Slf4j
@Profile({"asyncDownload"})
public class RabbitProducerMessageService implements ProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;
    private final HashGenerator<DownloadRequest> hashGenerator;
    public static final String JOB_ID = "jobId";

    public RabbitProducerMessageService(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<DownloadRequest> hashGenerator) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        this.hashGenerator = hashGenerator;
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

        if (!this.jobRepository.existsById(jobId)) {
            doSendMessage(downloadRequest, messageHeader, jobId);
            log.info("Message with jobId {} ready to be processed", jobId);
        } else {
            alreadyProcessed(jobId);
        }

        return jobId;
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
