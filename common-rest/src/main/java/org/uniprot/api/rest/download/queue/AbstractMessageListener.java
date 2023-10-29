package org.uniprot.api.rest.download.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.DownloadRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractMessageListener extends BaseAbstractMessageListener {

    private final MessageConverter converter;
    private final DownloadResultWriter downloadResultWriter;

    public AbstractMessageListener(
            MessageConverter converter,
            DownloadConfigProperties downloadConfigProperties,
            AsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties,
            DownloadJobRepository jobRepository,
            DownloadResultWriter downloadResultWriter,
            RabbitTemplate rabbitTemplate) {
        super(
                downloadConfigProperties,
                asyncDownloadQueueConfigProperties,
                jobRepository,
                rabbitTemplate);
        this.converter = converter;
        this.downloadResultWriter = downloadResultWriter;
    }

    @Override
    protected void processMessage(Message message, DownloadJob downloadJob) {
        DownloadRequest request = (DownloadRequest) this.converter.fromMessage(message);
        String jobId = downloadJob.getId();
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        Path resultFile =
                Paths.get(
                        downloadConfigProperties.getResultFilesFolder(),
                        jobId + FileType.GZIP.getExtension());
        // run the job if it has errored out
        if (isJobSeenBefore(downloadJob, idsFile, resultFile)) {
            if (downloadJob.getStatus() == JobStatus.RUNNING) {
                log.warn("The job {} is running by other thread", jobId);
            } else {
                log.info("The job {} is already processed", jobId);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED);
            }
        } else {
            updateStatusAndWriteResult(message, downloadJob, request, idsFile);
        }
    }

    protected void writeResult(
            DownloadRequest request, DownloadJob downloadJob, Path idsFile, String jobId, MediaType contentType) {
        try {
            writeSolrResult(request, downloadJob, idsFile, jobId);
            StoreRequest storeRequest = getStoreRequest(request);
            downloadResultWriter.writeResult(
                    request, downloadJob, idsFile, jobId, contentType, storeRequest, getDataType());
            log.info("Voldemort results saved for job {}", jobId);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    protected void writeSolrResult(DownloadRequest request, DownloadJob downloadJob, Path idsFile, String jobId)
            throws IOException {
        writeIdentifiers(idsFile, streamIds(request));
        updateTotalEntries(downloadJob, streamIds(request).count());
        log.info("Solr ids saved for job {}", jobId);
    }

    protected abstract StoreRequest getStoreRequest(DownloadRequest request);

    protected abstract Stream<String> streamIds(DownloadRequest request);

    protected abstract String getDataType();

    protected abstract void updateStatusAndWriteResult(
            Message message, DownloadJob downloadJob, DownloadRequest request, Path idsFile);
}
