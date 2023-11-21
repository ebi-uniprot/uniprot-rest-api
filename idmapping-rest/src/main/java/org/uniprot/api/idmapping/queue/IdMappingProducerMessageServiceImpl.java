package org.uniprot.api.idmapping.queue;

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
import org.uniprot.api.rest.request.HashGenerator;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
@Profile({"asyncDownload"})
public class IdMappingProducerMessageServiceImpl implements IdMappingProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;
    private final DownloadJobRepository jobRepository;
    private final HashGenerator<IdMappingDownloadRequest> idMappingHashGenerator;
    private static final String JOB_ID = "jobId";

    public IdMappingProducerMessageServiceImpl(
            MessageConverter converter,
            RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<IdMappingDownloadRequest> idMappingHashGenerator) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.jobRepository = downloadJobRepository;
        this.idMappingHashGenerator = idMappingHashGenerator;
    }

    @Override
    public String sendMessage(IdMappingDownloadRequest downloadRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String asyncDownloadJobId = idMappingHashGenerator.generateHash(downloadRequest);
        messageHeader.setHeader(JOB_ID, asyncDownloadJobId);

        if (Objects.isNull(downloadRequest.getFormat())) {
            downloadRequest.setFormat(APPLICATION_JSON_VALUE);
        }

        if (!this.jobRepository.existsById(asyncDownloadJobId)) {
            doSendMessage(downloadRequest, messageHeader, asyncDownloadJobId);
            log.info("Message with jobId {} ready to be processed", asyncDownloadJobId);
        } else {
            alreadyProcessed(asyncDownloadJobId);
        }
        return asyncDownloadJobId;
    }

    @Override
    public void alreadyProcessed(String jobId) {
        Optional<DownloadJob> downloadJob = this.jobRepository.findById(jobId);
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
