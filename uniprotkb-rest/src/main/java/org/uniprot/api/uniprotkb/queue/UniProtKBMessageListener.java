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

        if(optDownloadJob.isPresent()) {
            DownloadJob downloadJob = optDownloadJob.get();
            if(isMaxRetriedReached(message)){
                updateDownloadJob(message, downloadJob, JobStatus.ERROR);
                sendToUndeliveredQueue(jobId, message);
                return;
            }

            if(jobId != null){
                throw new MessageListenerException("test");
            }

            String contentType = message.getMessageProperties().getHeader("content-type");

            if (contentType == null) { //TODO: REMOVE IT
                contentType = "application/json";
            }

            Path idsFile = Paths.get(downloadConfigProperties.getFolder(), jobId);
            if (Files.notExists(idsFile)) {
                updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
                getAndWriteResult(request, idsFile, jobId, contentType);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            } else {
                log.info("The job {} is already processed", jobId);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            }


            // TESTING TO ALSO CREATE RESULT
            log.info("Message processed");
        } else {
            String errMsg = "Unable to find job id" + jobId + "in db";
            log.warn(errMsg);
            throw new MessageListenerException(errMsg);
        }
    }

    private boolean isMaxRetriedReached(Message message) {
        return getRetryCount(message) >= this.retryCount;
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
            log.warn("Unable to write file due to IOException for job id {}", jobId);
            try {
                Files.delete(idsFile);
            } catch (IOException e) {
                log.warn("Unable to delete file during IOException failure for job id {}", jobId);
                throw new MessageListenerException(e);
            }
            throw new MessageListenerException(ex);
        } catch (Exception e) {
            log.warn("Unable to write output to fs for job id {}", jobId);
            throw new MessageListenerException(e);
        }
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) throws IOException{
        Iterable<String> source = ids::iterator;
        Files.write(filePath, source, StandardOpenOption.CREATE);
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }

    private DownloadJob updateDownloadJob(Message message, DownloadJob downloadJob, JobStatus jobStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        downloadJob.setUpdated(now);
        downloadJob.setStatus(jobStatus);
        downloadJob.setRetried(getRetryCount(message));
        // TODO get error from x-death and set in job entry
//        downloadJob.setError(getLastError(message));
        return this.jobRepository.save(downloadJob);
    }

    private int getRetryCount(Message message) {
        List<Map<String, ?>> xDeaths = message.getMessageProperties().getHeader("x-death");
        int count = 0;
        if(Objects.nonNull(xDeaths)) {
            Map<String, ?> xDeath = xDeaths.get(0);
            if (Objects.nonNull(xDeath) && !xDeath.isEmpty()) {
                Long longCount = (Long) xDeath.get("count");
                count = longCount.intValue();
            }
        }
        return count;
    }
}
