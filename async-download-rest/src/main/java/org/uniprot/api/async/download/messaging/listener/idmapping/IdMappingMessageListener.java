package org.uniprot.api.async.download.messaging.listener.idmapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitTemplate;
import org.uniprot.api.async.download.messaging.listener.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.MessageListenerException;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.idmapping.AbstractIdMappingDownloadResultWriter;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingDownloadResultWriterFactory;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IdMappingMessageListener extends BaseAbstractMessageListener
        implements MessageListener {

    private final MessageConverter converter;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final DownloadConfigProperties downloadConfigProperties;
    private final IdMappingDownloadResultWriterFactory writerFactory;

    public IdMappingMessageListener(
            IdMappingDownloadConfigProperties idMappingDownloadConfigProperties,
            IdMappingAsyncDownloadQueueConfigProperties queueConfigProperties,
            IdMappingDownloadJobRepository jobRepository,
            IdMappingRabbitTemplate idMappingRabbitTemplate,
            MessageConverter converter,
            IdMappingJobCacheService idMappingJobCacheService,
            IdMappingDownloadResultWriterFactory writerFactory,
            IdMappingHeartbeatProducer heartBeatProducer,
            IdMappingAsyncDownloadFileHandler idMappingAsyncDownloadFileHandler) {
        super(
                idMappingDownloadConfigProperties,
                jobRepository,
                idMappingRabbitTemplate,
                heartBeatProducer,
                idMappingAsyncDownloadFileHandler,
                queueConfigProperties);
        this.converter = converter;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.downloadConfigProperties = idMappingDownloadConfigProperties;
        this.writerFactory = writerFactory;
    }

    @Override
    protected void processMessage(Message message, DownloadJob downloadJob) {
        IdMappingDownloadRequest request =
                (IdMappingDownloadRequest) this.converter.fromMessage(message);
        String asyncDownloadJobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

        IdMappingJob idMappingJob =
                idMappingJobCacheService.getCompletedJobAsResource(request.getJobId());

        Path resultFile =
                Paths.get(
                        downloadConfigProperties.getResultFilesFolder(),
                        asyncDownloadJobId + FileType.GZIP.getExtension());
        // run the job if it has errored out
        if (isJobSeenBefore(downloadJob, resultFile)) {
            if (downloadJob.getStatus() == JobStatus.RUNNING) {
                log.warn("The job {} is running by other thread", asyncDownloadJobId);
            } else {
                log.info("The job {} is already processed", asyncDownloadJobId);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            }
        } else {
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            updateTotalEntries(
                    downloadJob, idMappingJob.getIdMappingResult().getMappedIds().size());
            writeResult(request, idMappingJob, downloadJob, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, asyncDownloadJobId);
        }
    }

    private void writeResult(
            IdMappingDownloadRequest request,
            IdMappingJob idMappingJob,
            DownloadJob downloadJob,
            MediaType contentType) {
        try {
            AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> writer =
                    writerFactory.getResultWriter(idMappingJob.getIdMappingRequest().getTo());
            writer.writeResult(
                    request, idMappingJob.getIdMappingResult(), downloadJob, contentType);
            log.info("Voldemort results saved for job {}", downloadJob.getId());
        } catch (Exception ex) {
            logMessage(ex, downloadJob.getId());
            throw new MessageListenerException(ex);
        }
    }

    private static boolean isJobSeenBefore(DownloadJob downloadJob, Path resultFile) {
        return Files.exists(resultFile) && downloadJob.getStatus() != JobStatus.ERROR;
    }
}
