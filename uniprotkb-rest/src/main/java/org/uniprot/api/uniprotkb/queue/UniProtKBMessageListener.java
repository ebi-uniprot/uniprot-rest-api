package org.uniprot.api.uniprotkb.queue;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadResultWriter downloadResultWriter;
    private DownloadJobRepository jobRepository;

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.amqp.rabbit.rejectedQueueName}")
    private String rejectedQueueName;

    @Value("${spring.amqp.rabbit.retryMaxCount}")
    private Integer retryCount;

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
        UniProtKBStreamRequest request = (UniProtKBStreamRequest) converter.fromMessage(message);
        String jobId = message.getMessageProperties().getHeader("jobId");
        Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
        if(isMaxRetriedReached(message)){
            sendToUndeliveredQueue(jobId, message);
            return;
        }

        if(optDownloadJob.isPresent()) {
            DownloadJob downloadJob = optDownloadJob.get();
            String contentType = message.getMessageProperties().getHeader("content-type");

            if (contentType == null) { //TODO: REMOVE IT
                contentType = "application/json";
            }

            Path idsFile = Paths.get(downloadConfigProperties.getFolder(), jobId);
            if (Files.notExists(idsFile)) {
                updateDownloadJobWithRetry(downloadJob, JobStatus.RUNNING);
                getAndWriteResult(request, idsFile, jobId, contentType);
                updateDownloadJobWithRetry(downloadJob, JobStatus.FINISHED);
            } else {
                log.info("The job id {} is already processed", jobId);
                updateDownloadJob(downloadJob, JobStatus.FINISHED);
            }


            // TESTING TO ALSO CREATE RESULT
            log.info("Message processed");
        } else {
            // TODO should we replay the message?
            log.error("Unable to find job id {} in db", jobId);
        }

        // acknowledge the queue with failure/success
    }

    private boolean isMaxRetriedReached(Message message) {
        List<Map<String, ?>> xDeaths = message.getMessageProperties().getHeader("x-death");
        return !Objects.isNull(xDeaths) && getRetryCount(xDeaths.get(0)) >= this.retryCount;
    }

    private void sendToUndeliveredQueue(String jobId, Message message) {
        log.warn("Maximum retry {} reached for jobId {}. Sending to rejected queue", retryCount, jobId);
        this.rabbitTemplate.convertAndSend(rejectedQueueName, message);
    }

    private void getAndWriteResult(UniProtKBStreamRequest request, Path idsFile, String jobId, String contentType) {
        try {
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            MediaType mediaType = UniProtMediaType.valueOf(contentType);
            StoreRequest storeRequest = service.buildStoreRequest(request);
            downloadResultWriter.writeResult(request, idsFile, jobId, mediaType, storeRequest);
        } catch (IOException ex){
            // we should delete the file TODO
        } catch (Exception e) {
            //
            log.error("Unable to write output to fs", e);
            // TODO replay on queue
            // update retried count
        }
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) throws IOException{
        Iterable<String> source = ids::iterator;
        Files.write(filePath, source, StandardOpenOption.CREATE);
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }

    private void updateDownloadJobWithRetry(DownloadJob downloadJob, JobStatus status) {
        Failsafe.with(redisRetryPolicy())
                .onFailure(throwable -> log.error("Failed to update job {} with error", downloadJob.getId(), throwable))
                .get(() -> updateDownloadJob(downloadJob, status));
    }
    private DownloadJob updateDownloadJob(DownloadJob downloadJob, JobStatus jobStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        downloadJob.setUpdated(now);
        downloadJob.setStatus(jobStatus);
        return this.jobRepository.save(downloadJob);
    }

    // TODO make it a bean with config externalised
    public RetryPolicy<Object> redisRetryPolicy() {
        int redisRetryDelay = 5000;
        int maxRetries = 5;
        int maxRedisRetryDelay = redisRetryDelay * 8;
        return new RetryPolicy<>()
                .handle(Exception.class)
                .withBackoff(redisRetryDelay, maxRedisRetryDelay, ChronoUnit.MILLIS)
                .withMaxRetries(maxRetries)
                .onRetry(
                        e ->
                                log.warn(
                                        "Call to redis server failed. Failure #{}. Retrying. Failed error {}",
                                        e.getAttemptCount(), e.getLastFailure()));
    }

    private int getRetryCount(Map<String, ?> xDeath) {
        if (xDeath != null && !xDeath.isEmpty()) {
            Long count = (Long) xDeath.get("count");
            return count.intValue();
        }
        return 0;
    }
}
