package org.uniprot.api.idmapping.queue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.AbstractMessageListener;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.queue.MessageListenerException;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

@Slf4j
@Profile({"live", "asyncDownload"})
@Service("DownloadListener")
public class IdMappingMessageListener extends AbstractMessageListener implements MessageListener {

    private final MessageConverter converter;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final DownloadConfigProperties downloadConfigProperties;
    private final IdMappingDownloadResultWriterFactory writerFactory;

    @Value("${async.download.rejectedQueueName}")
    private String rejectedQueueName;

    @Value("${async.download.retryMaxCount}")
    private Integer maxRetryCount;

    @Value("${async.download.retryQueueName}")
    private String retryQueueName;

    public IdMappingMessageListener(
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository,
            RabbitTemplate rabbitTemplate,
            MessageConverter converter,
            IdMappingJobCacheService idMappingJobCacheService,
            IdMappingDownloadResultWriterFactory writerFactory) {
        super(downloadConfigProperties, jobRepository, rabbitTemplate);
        this.converter = converter;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.downloadConfigProperties = downloadConfigProperties;
        this.writerFactory = writerFactory;
    }

    @Override
    protected void processMessage(Message message, DownloadJob downloadJob) {
        IdMappingDownloadRequest request =
                (IdMappingDownloadRequest) this.converter.fromMessage(message);
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

        IdMappingJob idMappingJob = idMappingJobCacheService.getCompletedJobAsResource(jobId);

        Path resultFile =
                Paths.get(
                        downloadConfigProperties.getResultFilesFolder(),
                        jobId + FileType.GZIP.getExtension());
        // run the job if it has errored out
        if (isJobSeenBefore(downloadJob, resultFile)) {
            if (downloadJob.getStatus() == JobStatus.RUNNING) {
                log.warn("The job {} is running by other thread", jobId);
            } else {
                log.info("The job {} is already processed", jobId);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            }
        } else {
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            writeResult(request, idMappingJob, jobId, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
        }
    }

    private void writeResult(
            IdMappingDownloadRequest request,
            IdMappingJob idMappingJob,
            String jobId,
            MediaType contentType) {
        try {
            AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> writer =
                    writerFactory.getResultWriter(idMappingJob.getIdMappingRequest().getTo());
            writer.writeResult(request, idMappingJob.getIdMappingResult(), jobId, contentType);
            log.info("Voldemort results saved for job {}", jobId);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    private static boolean isJobSeenBefore(DownloadJob downloadJob, Path resultFile) {
        return Files.exists(resultFile) && downloadJob.getStatus() != JobStatus.ERROR;
    }

    @Override
    protected String getRejectedQueueName() {
        return this.rejectedQueueName;
    }

    @Override
    protected Integer getMaxRetryCount() {
        return this.maxRetryCount;
    }

    @Override
    protected String getRetryQueueName() {
        return this.retryQueueName;
    }
}