package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    static final String CURRENT_RETRIED_COUNT_HEADER = "x-uniprot-retry-count";
    static final String CURRENT_RETRIED_ERROR_HEADER = "x-uniprot-error";

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadResultWriter downloadResultWriter;
    private final DownloadJobRepository jobRepository;

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.amqp.rabbit.rejectedQueueName}")
    private String rejectedQueueName;

    @Value("${spring.amqp.rabbit.retryMaxCount}")
    private Integer maxRetryCount;

    @Value("${spring.amqp.rabbit.retryQueueName}")
    private String retryQueueName;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository,
            DownloadResultWriter downloadResultWriter,
            RabbitTemplate rabbitTemplate) {
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
        this.downloadResultWriter = downloadResultWriter;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void onMessage(Message message) {
        String jobId = null;
        DownloadJob downloadJob = null;
        log.info("################### Listener ");
        try {
            jobId = message.getMessageProperties().getHeader("jobId");
            Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
            String errorMsg = "Unable to find jobId " + jobId + " in db";
            downloadJob = optDownloadJob.orElseThrow(() -> new MessageListenerException(errorMsg));

            if (isMaxRetriedReached(message)) {
                updateDownloadJob(message, downloadJob, JobStatus.ERROR);
                sendToUndeliveredQueue(jobId, message);
                log.error(
                        "Message with job id {} discarded after max retry {}",
                        jobId,
                        this.maxRetryCount);
                return;
            }

            processMessage(message, downloadJob);

            log.info("Message with jobId {} processed successfully", jobId);
        } catch (Exception ex) {
            log.error("Download job id {} failed with error {}", jobId, ex.getStackTrace());
            Message updatedMessage = addAdditionalHeaders(message, ex);
            // TODO test what if this fails. need to x-death in this case
            updateDownloadJob(updatedMessage, downloadJob, JobStatus.ERROR);
            // TODO test what if this fails.
            sendToRetryQueue(jobId, updatedMessage);
        }
    }

    private void processMessage(Message message, DownloadJob downloadJob) {
        UniProtKBStreamRequest request =
                (UniProtKBStreamRequest) this.converter.fromMessage(message);
        String jobId = downloadJob.getId();
        String contentType =
                message.getMessageProperties().getHeader(UniProtKBDownloadController.CONTENT_TYPE);

        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        if (Files.notExists(idsFile)) {
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            getAndWriteResult(request, idsFile, jobId, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
        } else {
            log.info("The job {} is already processed", jobId);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
        }
    }

    private void getAndWriteResult(
            UniProtKBStreamRequest request, Path idsFile, String jobId, String contentType) {
        try {
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            MediaType mediaType = UniProtMediaType.valueOf(contentType);
            StoreRequest storeRequest = service.buildStoreRequest(request);
            downloadResultWriter.writeResult(request, idsFile, jobId, mediaType, storeRequest);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            log.warn("Unable to write file due to IOException for job id {}", jobId);
            try {
                Files.delete(idsFile);
            } catch (IOException e) {
                log.warn(
                        "Unable to delete file {} during IOException failure for job id {}",
                        idsFile.toFile().getName(),
                        jobId);
                throw new MessageListenerException(e);
            }
            throw new MessageListenerException(ex);
        }
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) throws IOException {
        Iterable<String> source = ids::iterator;
        Files.write(filePath, source, StandardOpenOption.CREATE);
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }

    private void updateDownloadJob(Message message, DownloadJob downloadJob, JobStatus jobStatus) {
        if (Objects.nonNull(downloadJob)) {
            LocalDateTime now = LocalDateTime.now();
            downloadJob.setUpdated(now);
            downloadJob.setStatus(jobStatus);
            downloadJob.setRetried(getRetryCount(message));
            String error = getLastError(message);
            downloadJob.setError(error);
            this.jobRepository.save(downloadJob);
        }
    }

    private boolean isMaxRetriedReached(Message message) {
        return getRetryCount(message) >= this.maxRetryCount;
    }

    private int getRetryCount(Message message) {
        Integer retryCount = message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER);
        return Objects.nonNull(retryCount) ? retryCount : 0;
    }

    private void sendToUndeliveredQueue(String jobId, Message message) {
        log.warn(
                "Maximum retry {} reached for jobId {}. Sending to rejected queue",
                this.maxRetryCount,
                jobId);
        this.rabbitTemplate.convertAndSend(rejectedQueueName, message);
    }

    private void sendToRetryQueue(String jobId, Message message) {
        log.warn("Sending message for jobId {} to retry queue", jobId);
        this.rabbitTemplate.convertAndSend(this.retryQueueName, message);
        log.info("Message with jobId {} sent to retry queue {}", jobId, this.retryQueueName);
    }

    private String getLastError(Message message) {
        if (Objects.nonNull(
                message.getMessageProperties().getHeader(CURRENT_RETRIED_ERROR_HEADER))) {
            return message.getMessageProperties()
                    .getHeader(CURRENT_RETRIED_ERROR_HEADER)
                    .toString();
        }
        return null;
    }

    Message addAdditionalHeaders(Message message, Exception ex) {
        MessageBuilder builder = MessageBuilder.fromMessage(message);
        Integer retryCount = message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER);
        if (Objects.nonNull(retryCount)) {
            retryCount++;
        } else {
            retryCount = 1;
        }

        String stackTrace =
                Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
        builder.setHeader(CURRENT_RETRIED_COUNT_HEADER, retryCount);
        builder.setHeader(CURRENT_RETRIED_ERROR_HEADER, stackTrace);
        return builder.build();
    }

    void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
