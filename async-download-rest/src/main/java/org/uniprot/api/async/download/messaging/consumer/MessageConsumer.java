package org.uniprot.api.async.download.messaging.consumer;

import static org.uniprot.api.rest.download.model.JobStatus.ERROR;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.mq.MessagingService;
import org.uniprot.api.async.download.service.JobService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MessageConsumer<T extends DownloadRequest, R extends DownloadJob>
        implements MessageListener {
    public static final String CURRENT_RETRIED_COUNT_HEADER = "x-uniprot-retry-count";
    private static final String CURRENT_RETRIED_ERROR_HEADER = "x-uniprot-error";
    protected static final String JOB_ID_HEADER = "jobId";
    protected static final String UPDATE_COUNT = "updateCount";
    protected static final String UPDATED = "updated";
    protected static final String PROCESSED_ENTRIES = "processedEntries";
    protected static final String STATUS = "status";
    protected static final String RETRIED = "retried";
    private final MessagingService messagingService;
    private final RequestProcessor<T> requestProcessor;
    private final FileHandler fileHandler;
    private final JobService<R> jobService;
    private final MessageConverter messageConverter;

    protected MessageConsumer(
            MessagingService messagingService,
            RequestProcessor<T> requestProcessor,
            FileHandler fileHandler,
            JobService<R> jobService,
            MessageConverter messageConverter) {
        this.messagingService = messagingService;
        this.requestProcessor = requestProcessor;
        this.fileHandler = fileHandler;
        this.jobService = jobService;
        this.messageConverter = messageConverter;
    }

    @Override
    public void onMessage(Message message) {
        String jobId = null;
        try {
            jobId = message.getMessageProperties().getHeader(JOB_ID_HEADER);
            log.info("Received job {} in listener", jobId);

            if (isMaxRetriesReached(message)) {
                rejectMessage(message, jobId);
            } else {
                String error = "Unable to find jobId " + jobId + " in db";
                R downloadJob =
                        jobService
                                .find(jobId)
                                .orElseThrow(() -> new MessageConsumerException(error));

                // run the job only if it has errored out
                if (isConsumedBefore(downloadJob)) {
                    if (downloadJob.getStatus() == RUNNING) {
                        log.warn("The job {} is running by other thread", jobId);
                    } else {
                        log.info("The job {} is already processed", jobId);
                    }
                } else {
                    cleanIfNecessary(downloadJob);
                    T request = (T) this.messageConverter.fromMessage(message);
                    request.setDownloadJobId(jobId);
                    requestProcessor.process(request);
                    log.info("Message with jobId {} processed successfully", jobId);
                }
            }
        } catch (Exception ex) {
            if (getRetryCountByBroker(message) <= messagingService.getMaxRetryCount()) {
                log.error("Download job id {} failed with error {}", jobId, ex.getMessage());
                Message updatedMessage = addAdditionalHeaders(message, ex);
                jobService.update(
                        jobId, Map.of(RETRIED, getRetryCount(updatedMessage), STATUS, ERROR));
                log.warn("Sending message for jobId {} to retry queue", jobId);
                messagingService.sendToRetry(updatedMessage);
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

    protected boolean isConsumedBefore(R downloadJob) {
        return fileHandler.areIdAndResultFilesPresent(downloadJob.getId())
                && ERROR != downloadJob.getStatus();
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
        int retryCount =
                (Integer)
                                Optional.ofNullable(
                                                message.getMessageProperties()
                                                        .getHeader(CURRENT_RETRIED_COUNT_HEADER))
                                        .orElse(0)
                        + 1;

        String stackTrace =
                Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
        builder.setHeader(CURRENT_RETRIED_COUNT_HEADER, retryCount);
        builder.setHeader(CURRENT_RETRIED_ERROR_HEADER, stackTrace);
        return builder.build();
    }

    private boolean isMaxRetriesReached(Message message) {
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
        fileHandler.deleteAllFiles(jobId);
    }
}
