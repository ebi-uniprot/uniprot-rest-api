package org.uniprot.api.rest.download.queue;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.request.StreamRequest;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service
@Slf4j
public class RabbitProducerMessageService implements ProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;

    public RabbitProducerMessageService(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
    }

    @Override
    public void sendMessage(StreamRequest streamRequest, MessageProperties messageHeader) {
        String jobId = messageHeader.getHeader("jobId");
        if (!this.jobRepository.existsById(jobId)) {
            doSendMessage(streamRequest, messageHeader, jobId);
        } else {
            logAlreadyProcessed(jobId);
        }
    }

    public void logAlreadyProcessed(String jobId) {
        log.info("Job is already processed {}", jobId);
    }

    private void doSendMessage(
            StreamRequest streamRequest, MessageProperties messageHeader, String jobId) {
        Message message = converter.toMessage(streamRequest, messageHeader);
        // write to redis and put on queue
        createDownloadJob(jobId, streamRequest);
        try {
            this.rabbitTemplate.send(message);
        } catch (AmqpException amqpException) {
            log.error("Unable to send message to the queue with exception {}", amqpException);
            this.jobRepository.deleteById(jobId);
            throw amqpException;
        }
    }

    private void createDownloadJob(String jobId, StreamRequest streamRequest) {
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(streamRequest.getQuery())
                .fields(streamRequest.getFields())
                .sort(streamRequest.getSort())
                .created(now)
                .updated(now);
        this.jobRepository.save(jobBuilder.build());
    }
}
