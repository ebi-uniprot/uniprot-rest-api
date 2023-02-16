package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBDownloadRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Profile({"live", "asyncDownload"})
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

    static AtomicInteger times = new AtomicInteger(0);

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
        log.info(
                this
                        + " #################### times called "
                        + times.incrementAndGet()); // TODO remove
        String jobId = null;
        DownloadJob downloadJob = null;
        try {
            jobId = message.getMessageProperties().getHeader("jobId");
            if (isMaxRetriedReached(message)) {
                sendToUndeliveredQueue(jobId, message);
                log.error(
                        "Message with job id {} discarded after max retry {}",
                        jobId,
                        this.maxRetryCount);
                return;
            }

            Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
            String errorMsg = "Unable to find jobId " + jobId + " in db";
            downloadJob = optDownloadJob.orElseThrow(() -> new MessageListenerException(errorMsg));

            processMessage(message, downloadJob);

            log.info("Message with jobId {} processed successfully", jobId);
        } catch (Exception ex) {
            if (getRetryCountByBroker(message) <= this.maxRetryCount) {
                log.error("Download job id {} failed with error {}", jobId, ex.getStackTrace());
                Message updatedMessage = addAdditionalHeaders(message, ex);
                updateDownloadJob(updatedMessage, downloadJob, JobStatus.ERROR);
                sendToRetryQueue(jobId, updatedMessage);
            } else {
                // the flow should not come here, letting the flow complete without rethrowing
                // exception to avoid poison message
                log.error(
                        "Message with jobId {} failed due to error {} which is not handled",
                        jobId,
                        ex);
            }
        }
    }

    private void processMessage(Message message, DownloadJob downloadJob) {
        UniProtKBDownloadRequest request =
                (UniProtKBDownloadRequest) this.converter.fromMessage(message);
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getContentType());
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        Path resultFile = Paths.get(downloadConfigProperties.getResultFilesFolder(), jobId);
        if (Files.exists(idsFile) && Files.exists(resultFile)) {
            log.info("The job {} is already processed", jobId);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
        } else {
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            writeResult(request, idsFile, jobId, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
        }
    }

    private void writeResult(
            DownloadRequest request, Path idsFile, String jobId, MediaType contentType) {
        try {
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            StoreRequest storeRequest = service.buildStoreRequest(request);
            downloadResultWriter.writeResult(request, idsFile, jobId, contentType, storeRequest);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId, contentType);
            throw new MessageListenerException(ex);
        }
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) throws IOException {
        Iterable<String> source = ids::iterator;
        Files.write(filePath, source, StandardOpenOption.CREATE);
    }

    Stream<String> streamIds(DownloadRequest request) {
        return service.streamIds(request);
    }

    private void updateDownloadJob(Message message, DownloadJob downloadJob, JobStatus jobStatus) {
        updateDownloadJob(message, downloadJob, jobStatus, null);
    }

    private void updateDownloadJob(
            Message message, DownloadJob downloadJob, JobStatus jobStatus, String resultFile) {
        if (Objects.nonNull(downloadJob)) {
            LocalDateTime now = LocalDateTime.now();
            downloadJob.setUpdated(now);
            downloadJob.setStatus(jobStatus);
            downloadJob.setRetried(getRetryCount(message));
            String error = getLastError(message);
            downloadJob.setError(error);
            downloadJob.setResultFile(resultFile);
            this.jobRepository.save(downloadJob);
            dummyMethodForTesting(downloadJob.getId(), jobStatus);
        }
    }

    private boolean isMaxRetriedReached(Message message) {
        return getRetryCount(message) >= this.maxRetryCount;
    }

    private int getRetryCount(Message message) {
        Integer retryCountHandledError =
                message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER);
        Integer retryCountUnhandledError = getRetryCountByBroker(message);
        int retryCount =
                Math.max(
                        Objects.nonNull(retryCountHandledError) ? retryCountHandledError : 0,
                        retryCountUnhandledError);
        log.info("#################### retryCount " + retryCount);
        return retryCount;
    }

    private int getRetryCountByBroker(Message message) {
        int retriedByBroker = 0;
        List<Map<String, ?>> xDeaths = message.getMessageProperties().getHeader("x-death");
        if (Objects.nonNull(xDeaths)) {
            Map<String, ?> xDeathCount = xDeaths.get(0);
            if (Objects.nonNull(xDeathCount) && !xDeathCount.isEmpty()) {
                retriedByBroker = ((Long) xDeathCount.get("count")).intValue();
            }
        }

        if (retriedByBroker > 0) {
            String jobId = message.getMessageProperties().getHeader("jobId");
            log.error(
                    "This x-death retry count {} should be null, something unexpected has happened during processing of job {}",
                    retriedByBroker,
                    jobId);
        }

        return retriedByBroker;
    }

    private void sendToUndeliveredQueue(String jobId, Message message) {
        log.warn(
                "Maximum retry {} reached for jobId {}. Sending to rejected queue",
                this.maxRetryCount,
                jobId);
        this.rabbitTemplate.convertAndSend(rejectedQueueName, message);
        log.info("Message with jobId {} sent to rejected queue {}", jobId, this.rejectedQueueName);
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

    void dummyMethodForTesting(String jobId, JobStatus jobStatus) {
        // do nothing
    }

    private void logMessageAndDeleteFile(Exception ex, String jobId, MediaType contentType) {
        log.warn("Unable to write file due to error for job id {}", jobId);
        log.warn(ex.getMessage());
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        deleteFile(idsFile, jobId);
        String resultFileName = jobId + "." + UniProtMediaType.getFileExtension(contentType);
        Path resultFile =
                Paths.get(downloadConfigProperties.getResultFilesFolder(), resultFileName);
        deleteFile(resultFile, jobId);
    }

    private static void deleteFile(Path file, String jobId) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn(
                    "Unable to delete file {} during IOException failure for job id {}",
                    file.toFile().getName(),
                    jobId);
            throw new MessageListenerException(e);
        }
    }

    @Override
    public String toString() {
        return "UniProtKBMessageListener{"
                + "converter="
                + converter.hashCode()
                + ", service="
                + service.hashCode()
                + ", downloadConfigProperties="
                + downloadConfigProperties.hashCode()
                + ", downloadResultWriter="
                + downloadResultWriter.hashCode()
                + ", jobRepository="
                + jobRepository.hashCode()
                + ", rabbitTemplate="
                + rabbitTemplate.hashCode()
                + '}';
    }
}
