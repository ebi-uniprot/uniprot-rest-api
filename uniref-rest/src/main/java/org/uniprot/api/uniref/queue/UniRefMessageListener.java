package org.uniprot.api.uniref.queue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.AbstractMessageListener;
import org.uniprot.api.rest.download.queue.AsyncDownloadQueueConfigProperties;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.queue.MessageListenerException;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniref.request.UniRefDownloadRequest;
import org.uniprot.api.uniref.service.UniRefEntryLightService;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Profile({"live", "asyncDownload"})
@Service("DownloadListener")
@Slf4j
public class UniRefMessageListener extends AbstractMessageListener implements MessageListener {
    private static final String DATA_TYPE = "uniref";
    private final MessageConverter converter;
    private final UniRefEntryLightService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadResultWriter downloadResultWriter;

    public UniRefMessageListener(
            MessageConverter converter,
            UniRefEntryLightService service,
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
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.downloadResultWriter = downloadResultWriter;
    }

    protected void processMessage(Message message, DownloadJob downloadJob) {
        UniRefDownloadRequest request = (UniRefDownloadRequest) this.converter.fromMessage(message);
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
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
            updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
            writeResult(request, idsFile, jobId, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
        }
    }

    private void writeResult(
            DownloadRequest request, Path idsFile, String jobId, MediaType contentType) {
        try {
            writeSolrResult(request, idsFile, jobId);
            StoreRequest.StoreRequestBuilder storeRequest = StoreRequest.builder();
            storeRequest.addLineage(false);
            StoreRequest storeReq = storeRequest.build();
            downloadResultWriter.writeResult(
                    request, idsFile, jobId, contentType, storeReq, DATA_TYPE);
            log.info("Voldemort results saved for job {}", jobId);
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    private void writeSolrResult(DownloadRequest request, Path idsFile, String jobId)
            throws IOException {
        Stream<String> ids = streamIds(request);
        saveIdsInTempFile(idsFile, ids);
        log.info("Solr ids saved for job {}", jobId);
    }

    Stream<String> streamIds(DownloadRequest request) {
        return service.streamIds(request);
    }
}
