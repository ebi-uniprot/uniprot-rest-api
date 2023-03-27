package org.uniprot.api.uniprotkb.queue;

import static org.uniprot.api.rest.download.queue.RabbitProducerMessageService.JOB_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
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
import org.uniprot.api.rest.download.queue.EmbeddingsQueueConfigProperties;
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
@Service("DownloadListener")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    static final String CURRENT_RETRIED_COUNT_HEADER = "x-uniprot-retry-count";
    static final String CURRENT_RETRIED_ERROR_HEADER = "x-uniprot-error";
    public static final String JOB_ID_HEADER = "jobId";

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadResultWriter downloadResultWriter;
    private final DownloadJobRepository jobRepository;

    private final RabbitTemplate rabbitTemplate;

    @Value("${async.download.rejectedQueueName}")
    private String rejectedQueueName;

    @Value("${async.download.retryMaxCount}")
    private Integer maxRetryCount;

    @Value("${async.download.retryQueueName}")
    private String retryQueueName;

    private final EmbeddingsQueueConfigProperties embeddingsQueueConfigProps;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository,
            DownloadResultWriter downloadResultWriter,
            RabbitTemplate rabbitTemplate,
            EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties) {
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
        this.downloadResultWriter = downloadResultWriter;
        this.rabbitTemplate = rabbitTemplate;
        this.embeddingsQueueConfigProps = embeddingsQueueConfigProperties;
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
                        this.maxRetryCount);
                return;
            }

            Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
            String errorMsg = "Unable to find jobId " + jobId + " in db";
            downloadJob = optDownloadJob.orElseThrow(() -> new MessageListenerException(errorMsg));

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
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        Path resultFile = Paths.get(downloadConfigProperties.getResultFilesFolder(), jobId);
        // run the job if it has errored out
        if (isJobSeenBefore(downloadJob, idsFile, resultFile)) {
            if (downloadJob.getStatus() == JobStatus.RUNNING) {
                log.warn("The job {} is running by other thread", jobId);
            } else {
                log.info("The job {} is already processed", jobId);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            }
        } else {
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
                processH5Message(request, idsFile, jobId);
                updateDownloadJob(message, downloadJob, JobStatus.UNFINISHED);
            } else {
                writeResult(request, idsFile, jobId, contentType);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
            }
        }
    }

    private void processH5Message(UniProtKBDownloadRequest request, Path idsFile, String jobId) {
        writeSolrResult(request, idsFile, jobId);
        sendMessageToEmbeddingsQueue(jobId);
    }

    private static boolean isJobSeenBefore(DownloadJob downloadJob, Path idsFile, Path resultFile) {
        return Files.exists(idsFile)
                && Files.exists(resultFile)
                && downloadJob.getStatus() != JobStatus.ERROR;
    }

    private void writeResult(
            DownloadRequest request, Path idsFile, String jobId, MediaType contentType) {
        try {
            writeSolrResult(request, idsFile, jobId);
            StoreRequest storeRequest = service.buildStoreRequest(request);
            downloadResultWriter.writeResult(request, idsFile, jobId, contentType, storeRequest);
            log.info("Voldemort results saved for job {}", jobId);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    private void writeSolrResult(DownloadRequest request, Path idsFile, String jobId) {
        try {
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            log.info("Solr ids saved for job {}", jobId);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
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

    private void logMessageAndDeleteFile(Exception ex, String jobId) {
        log.warn("Unable to write file due to error for job id {}", jobId);
        log.warn(ex.getMessage());
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        deleteFile(idsFile, jobId);
        String resultFileName = jobId + ".gz";
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

    private void sendMessageToEmbeddingsQueue(String jobId) {
        log.info(
                "Sending h5 message to embeddings queue for further processing for jobId {}",
                jobId);
        MessageProperties msgProps = new MessageProperties();
        msgProps.setHeader(JOB_ID, jobId);
        Message message = new Message(new byte[] {}, msgProps);
        this.rabbitTemplate.send(
                this.embeddingsQueueConfigProps.getExchangeName(),
                this.embeddingsQueueConfigProps.getRoutingKey(),
                message);
        log.info(
                "Message with jobId {} sent to embeddings queue {}",
                jobId,
                this.embeddingsQueueConfigProps.getQueueName());
    }
}
