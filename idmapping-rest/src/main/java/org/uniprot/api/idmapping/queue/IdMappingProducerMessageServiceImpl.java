package org.uniprot.api.idmapping.queue;

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
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

@Service
@Slf4j
@Profile({"asyncDownload"})
public class IdMappingProducerMessageServiceImpl implements IdMappingProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;
    private static final String JOB_ID = "jobId";

    public IdMappingProducerMessageServiceImpl(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
    }

    @Override
    public String sendMessage(IdMappingDownloadRequest downloadRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = downloadRequest.getJobId();
        messageHeader.setHeader(JOB_ID, jobId);

        if (Objects.isNull(downloadRequest.getFormat())) {
            downloadRequest.setFormat(APPLICATION_JSON_VALUE);
        }

        if (!this.jobRepository.existsById(jobId)) {
            doSendMessage(downloadRequest, messageHeader);
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
            IdMappingDownloadRequest downloadRequest, MessageProperties messageHeader) {
        String jobId = downloadRequest.getJobId();
        Message message = converter.toMessage(downloadRequest, messageHeader);
        // write to redis and put on queue
        createDownloadJob(downloadRequest);
        log.info("Message with jobId {} created in redis", jobId);
        try {
            this.rabbitTemplate.send(message);
            log.info("Message with jobId sent to download queue {}", jobId);
        } catch (AmqpException amqpException) {
            log.error("Unable to send message to the queue with exception", amqpException);
            this.jobRepository.deleteById(jobId);
            throw amqpException;
        }
    }

    private void createDownloadJob(IdMappingDownloadRequest downloadRequest) {
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(downloadRequest.getJobId()).status(JobStatus.NEW);
        jobBuilder
                .fields(downloadRequest.getFields())
                .format(downloadRequest.getFormat())
                .created(now)
                .updated(now);
        this.jobRepository.save(jobBuilder.build());
    }
}
