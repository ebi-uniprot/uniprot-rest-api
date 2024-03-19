package org.uniprot.api.async.download.messaging.listener.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

@Slf4j
public abstract class BaseAbstractMessageListener implements MessageListener {

    public static final String CURRENT_RETRIED_COUNT_HEADER = "x-uniprot-retry-count";
    public static final String CURRENT_RETRIED_ERROR_HEADER = "x-uniprot-error";
    public static final String JOB_ID_HEADER = "jobId";
    private static final String UPDATE_COUNT = "updateCount";
    private static final String UPDATED = "updated";
    private static final String PROCESSED_ENTRIES = "processedEntries";

    protected final DownloadConfigProperties downloadConfigProperties;

    private final DownloadJobRepository jobRepository;

    protected final RabbitTemplate rabbitTemplate;
    private final HeartbeatProducer heartbeatProducer;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    private final AsyncDownloadQueueConfigProperties queueConfigProperties;

    public BaseAbstractMessageListener(
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository,
            RabbitTemplate rabbitTemplate,
            HeartbeatProducer heartbeatProducer,
            AsyncDownloadFileHandler asyncDownloadFileHandler,
            AsyncDownloadQueueConfigProperties queueConfigProperties) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.heartbeatProducer = heartbeatProducer;
        this.asyncDownloadFileHandler = asyncDownloadFileHandler;
        this.queueConfigProperties = queueConfigProperties;
    }

    @Override
    public void onMessage(Message message) {
        String jobId = null;
        DownloadJob downloadJob = null;
        try {
            jobId = message.getMessageProperties().getHeader(JOB_ID_HEADER);
            log.info("Received job {} in listener", jobId);
            if (isMaxRetriedReached(message)) {
                sendToUndeliveredQueue(jobId, message);
                log.error(
                        "Message with job id {} discarded after max retry {}",
                        jobId,
                        getMaxRetryCount());
                cleanFilesAndResetCounts(jobId);
                return;
            }

            Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
            String errorMsg = "Unable to find jobId " + jobId + " in db";
            downloadJob = optDownloadJob.orElseThrow(() -> new MessageListenerException(errorMsg));

            if (downloadJob.getRetried() > 0) {
                cleanFilesAndResetCounts(jobId);
            }

            processMessage(message, downloadJob);
            // get the fresh object from redis
            optDownloadJob = this.jobRepository.findById(jobId);
            if (optDownloadJob.isPresent()) {
                downloadJob = optDownloadJob.get();
                if (downloadJob.getStatus() == JobStatus.FINISHED) {
                    log.info("Message with jobId {} processed successfully", jobId);
                }
            }
        } catch (Exception ex) {
            if (getRetryCountByBroker(message) <= getMaxRetryCount()) {
                log.error("Download job id {} failed with error {}", jobId, ex.getMessage());
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

    public Message addAdditionalHeaders(Message message, Exception ex) {
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

    public void dummyMethodForTesting(String jobId, JobStatus jobStatus) {
        // do nothing
    }

    protected abstract void processMessage(Message message, DownloadJob downloadJob);

    protected static boolean isJobSeenBefore(
            DownloadJob downloadJob, Path idsFile, Path resultFile) {
        return Files.exists(idsFile)
                && Files.exists(resultFile)
                && downloadJob.getStatus() != JobStatus.ERROR;
    }

    protected void writeIdentifiers(Path filePath, Stream<String> ids, DownloadJob downloadJob)
            throws IOException {
        try (BufferedWriter writer =
                Files.newBufferedWriter(
                        filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            Iterable<String> iterator = ids::iterator;
            for (String id : iterator) {
                writer.append(id);
                writer.newLine();
                heartbeatProducer.createForIds(downloadJob);
            }
        } finally {
            heartbeatProducer.stop(downloadJob.getId());
        }
    }

    protected void updateDownloadJob(
            Message message, DownloadJob downloadJob, JobStatus jobStatus) {
        updateDownloadJob(message, downloadJob, jobStatus, null);
    }

    protected void updateDownloadJob(
            Message message, DownloadJob downloadJob, JobStatus jobStatus, String resultFile) {
        if (Objects.nonNull(downloadJob)) {
            int retryCount = getRetryCount(message);
            String error = getLastError(message);
            updateDownloadJob(downloadJob, jobStatus, error, retryCount, resultFile);
        }
    }

    protected void updateTotalEntries(DownloadJob downloadJob, long totalEntries) {
        downloadJob.setTotalEntries(totalEntries);
        downloadJob.setUpdated(LocalDateTime.now());
        jobRepository.save(downloadJob);
    }

    protected void updateDownloadJob(
            DownloadJob downloadJob,
            JobStatus jobStatus,
            String error,
            int retryCount,
            String resultFile) {
        if (Objects.nonNull(downloadJob)) {
            LocalDateTime now = LocalDateTime.now();
            downloadJob.setUpdated(now);
            downloadJob.setStatus(jobStatus);
            downloadJob.setRetried(retryCount);
            downloadJob.setError(error);
            downloadJob.setResultFile(resultFile);
            this.jobRepository.save(downloadJob);
            dummyMethodForTesting(downloadJob.getId(), jobStatus);
        }
    }

    protected void logMessage(Exception ex, String jobId) {
        log.warn("Unable to write file due to error for job id {}", jobId);
        log.warn(ex.getMessage());
    }

    private void cleanFilesAndResetCounts(String jobId) {
        jobRepository.update(
                jobId,
                Map.of(
                        UPDATE_COUNT, 0L,
                        UPDATED, LocalDateTime.now(),
                        PROCESSED_ENTRIES, 0L));
        asyncDownloadFileHandler.deleteAllFiles(jobId);
    }

    private boolean isMaxRetriedReached(Message message) {
        return getRetryCount(message) >= getMaxRetryCount();
    }

    protected int getMaxRetryCount() {
        return this.queueConfigProperties.getRetryMaxCount();
    }

    protected String getRejectedQueueName() {
        return this.queueConfigProperties.getRejectedQueueName();
    }

    protected String getRetryQueueName() {
        return this.queueConfigProperties.getRetryQueueName();
    }

    private int getRetryCount(Message message) {
        Integer retryCountHandledError =
                message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER);
        Integer retryCountUnhandledError = getRetryCountByBroker(message);
        int retryCount =
                Math.max(
                        Objects.nonNull(retryCountHandledError) ? retryCountHandledError : 0,
                        retryCountUnhandledError);
        String jobId = message.getMessageProperties().getHeader(JOB_ID_HEADER);
        log.info("retryCount for job {} is {}", jobId, retryCount);
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
            String jobId = message.getMessageProperties().getHeader(JOB_ID_HEADER);
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
                getMaxRetryCount(),
                jobId);
        this.rabbitTemplate.convertAndSend(getRejectedQueueName(), message);
        log.info("Message with jobId {} sent to rejected queue {}", jobId, getRejectedQueueName());
    }

    private void sendToRetryQueue(String jobId, Message message) {
        log.warn("Sending message for jobId {} to retry queue", jobId);
        this.rabbitTemplate.convertAndSend(getRetryQueueName(), message);
        log.info("Message with jobId {} sent to retry queue {}", jobId, getRetryQueueName());
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
}
