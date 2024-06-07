package org.uniprot.api.async.download.messaging.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.mq.MessagingService;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.download.model.JobStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class ContentBasedAndRetriableMessageConsumer<
        T extends DownloadRequest, R extends DownloadJob>
        implements MessageListener {
    protected static final String CURRENT_RETRIED_COUNT_HEADER = "x-uniprot-retry-count";
    private static final String CURRENT_RETRIED_ERROR_HEADER = "x-uniprot-error";
    protected static final String JOB_ID_HEADER = "jobId";
    protected static final String UPDATE_COUNT = "updateCount";
    protected static final String UPDATED = "updated";
    protected static final String PROCESSED_ENTRIES = "processedEntries";
    protected static final String STATUS = "status";
    protected static final String RETRY_COUNT = "retryCount";
    private final MessagingService messagingService;
    private final RequestProcessor<T> requestProcessor;
    private final AsyncDownloadFileHandler asyncDownloadFileHandler;
    private final JobService<R> jobService;
    private final MessageConverter messageConverter;

    protected ContentBasedAndRetriableMessageConsumer(
            MessagingService messagingService,
            RequestProcessor<T> requestProcessor,
            AsyncDownloadFileHandler asyncDownloadFileHandler,
            JobService<R> jobService,
            MessageConverter messageConverter) {
        this.messagingService = messagingService;
        this.requestProcessor = requestProcessor;
        this.asyncDownloadFileHandler = asyncDownloadFileHandler;
        this.jobService = jobService;
        this.messageConverter = messageConverter;
    }

    @Override
    public void onMessage(Message message) {
        String id = null;
        try {
            id = message.getMessageProperties().getHeader(JOB_ID_HEADER);
            log.info("Received job {} in listener", id);

            if (isMaxRetriedReached(message)) {
                rejectMessage(message, id);
            } else {
                String error = "Unable to find jobId " + id + " in db";
                DownloadJob downloadJob =
                        jobService
                                .find(id)
                                .orElseThrow(() -> new MessageConsumerException(error));

                // run the job only if it has errored out
                if (isJobSeenBefore(id) && JobStatus.ERROR != downloadJob.getStatus()) {
                    if (downloadJob.getStatus() == JobStatus.RUNNING) {
                        log.warn("The job {} is running by other thread", id);
                    } else {
                        log.info("The job {} is already processed", id);
                    }
                } else {
                    cleanIfNecessary(downloadJob);
                    jobService.update(id, Map.of(STATUS, JobStatus.RUNNING));
                    T request = (T) this.messageConverter.fromMessage(message);
                    request.setId(id);
                    requestProcessor.process(request);
                    jobService.update(id, Map.of(STATUS, JobStatus.FINISHED));
                    log.info("Message with jobId {} processed successfully", id);
                }
            }
        } catch (Exception ex) {
            if (getRetryCountByBroker(message) <= messagingService.getMaxRetryCount()) {
                log.error("Download job id {} failed with error {}", id, ex.getMessage());
                Message updatedMessage = addAdditionalHeaders(message, ex);
                jobService.update(
                        id,
                        Map.of(
                                RETRY_COUNT,
                                getRetryCount(updatedMessage),
                                STATUS,
                                JobStatus.ERROR));
                log.warn("Sending message for jobId {} to retry queue", id);
                messagingService.sendToRetry(updatedMessage);
            } else {
                // the flow should not come here, letting the flow complete without rethrowing
                // exception to avoid poison message
                log.error(
                        "Message with jobId {} failed due to error {} which is not handled",
                        id,
                        ex);
            }
        }
    }

    private boolean isJobSeenBefore(String jobId) {
        return asyncDownloadFileHandler.areAllFilesExist(jobId);
    }

    private void cleanIfNecessary(DownloadJob downloadJob) {
        if (downloadJob.getRetried() > 0) {
            cleanFilesAndResetCounts(downloadJob.getId());
        }
    }

    private void rejectMessage(Message message, String jobId) {
        log.warn(
                "Maximum retry {} reached for jobId {}. Sending to rejected queue",
                messagingService.getMaxRetryCount(),
                jobId);
        messagingService.sendToRejected(message);
        log.error(
                "Message with job id {} discarded after max retry {}",
                jobId,
                messagingService.getMaxRetryCount());
        cleanFilesAndResetCounts(jobId);
    }

    private Message addAdditionalHeaders(Message message, Exception ex) {
        MessageBuilder builder = MessageBuilder.fromMessage(message);
        int retryCount = (Integer) Optional.ofNullable(message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER)).orElse(0) + 1;

        String stackTrace =
                Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
        builder.setHeader(CURRENT_RETRIED_COUNT_HEADER, retryCount);
        builder.setHeader(CURRENT_RETRIED_ERROR_HEADER, stackTrace);
        return builder.build();
    }

    private boolean isMaxRetriedReached(Message message) {
        return getRetryCount(message) >= messagingService.getMaxRetryCount();
    }

    private int getRetryCount(Message message) {
        Integer retryCountHandledError =
                message.getMessageProperties().getHeader(CURRENT_RETRIED_COUNT_HEADER);
        int retryCountUnhandledError = getRetryCountByBroker(message);
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

    private void cleanFilesAndResetCounts(String jobId) {
        jobService.update(
                jobId,
                Map.of(
                        UPDATE_COUNT, 0L,
                        UPDATED, LocalDateTime.now(),
                        PROCESSED_ENTRIES, 0L));
        asyncDownloadFileHandler.deleteAllFiles(jobId);
    }
}
