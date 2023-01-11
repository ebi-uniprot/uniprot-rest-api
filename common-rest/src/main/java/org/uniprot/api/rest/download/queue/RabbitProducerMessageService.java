package org.uniprot.api.rest.download.queue;

import java.sql.Timestamp;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
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

    private DownloadJobRepository jobRepository;

    private HashGenerator<StreamRequest> hashGenerator;

    private static final String SALT_STR = "UNIPROT_DOWNLOAD_SALT"; // TODO Parametrized it

    public RabbitProducerMessageService(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        // TODO make it a bean without new
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
    }

    @Override
    public String sendMessage(StreamRequest streamRequest) {
        String jobId = this.hashGenerator.generateHash(streamRequest);
        if (!this.jobRepository.existsById(jobId)) {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setHeader("jobId", jobId);
            Message message = converter.toMessage(streamRequest, messageProperties);
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
        return jobId;
    }

    private void createDownloadJob(String jobId, StreamRequest streamRequest) {
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        Timestamp now = new Timestamp(System.currentTimeMillis());
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
