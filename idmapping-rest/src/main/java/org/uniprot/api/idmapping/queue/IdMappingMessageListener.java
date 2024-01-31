package org.uniprot.api.idmapping.queue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.heartbeat.HeartBeatProducer;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.AsyncDownloadQueueConfigProperties;
import org.uniprot.api.rest.download.queue.BaseAbstractMessageListener;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.queue.MessageListenerException;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

@Slf4j
@Profile({"live", "asyncDownload"})
@Service("DownloadListener")
public class IdMappingMessageListener extends BaseAbstractMessageListener
        implements MessageListener {

    private final MessageConverter converter;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final DownloadConfigProperties downloadConfigProperties;
    private final IdMappingDownloadResultWriterFactory writerFactory;

    public IdMappingMessageListener(
            DownloadConfigProperties downloadConfigProperties,
            AsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties,
            DownloadJobRepository jobRepository,
            RabbitTemplate rabbitTemplate,
            MessageConverter converter,
            IdMappingJobCacheService idMappingJobCacheService,
            IdMappingDownloadResultWriterFactory writerFactory,
            HeartBeatProducer heartBeatProducer) {
        super(
                downloadConfigProperties,
                asyncDownloadQueueConfigProperties,
                jobRepository,
                rabbitTemplate,
                heartBeatProducer);
        this.converter = converter;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.downloadConfigProperties = downloadConfigProperties;
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
            logMessageAndDeleteFile(ex, downloadJob.getId());
            throw new MessageListenerException(ex);
        }
    }

    private static boolean isJobSeenBefore(DownloadJob downloadJob, Path resultFile) {
        return Files.exists(resultFile) && downloadJob.getStatus() != JobStatus.ERROR;
    }
}
