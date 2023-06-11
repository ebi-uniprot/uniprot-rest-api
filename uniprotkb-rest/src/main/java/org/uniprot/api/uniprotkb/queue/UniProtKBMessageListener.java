package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
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
import org.uniprot.api.uniprotkb.controller.request.UniProtKBDownloadRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.queue.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Profile({"live", "asyncDownload"})
@Service("DownloadListener")
@Slf4j
public class UniProtKBMessageListener extends AbstractMessageListener implements MessageListener {
    public static final String H5_LIMIT_EXCEED_MSG = "Embeddings Limit Exceeded(%s)";
    private static final String DATA_TYPE = "uniprotkb";
    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadResultWriter downloadResultWriter;
    private final EmbeddingsQueueConfigProperties embeddingsQueueConfigProps;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            DownloadConfigProperties downloadConfigProperties,
            AsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties,
            DownloadJobRepository jobRepository,
            DownloadResultWriter downloadResultWriter,
            RabbitTemplate rabbitTemplate,
            EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties) {
        super(
                downloadConfigProperties,
                asyncDownloadQueueConfigProperties,
                jobRepository,
                rabbitTemplate);
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.downloadResultWriter = downloadResultWriter;
        this.embeddingsQueueConfigProps = embeddingsQueueConfigProperties;
    }

    protected void processMessage(Message message, DownloadJob downloadJob) {
        UniProtKBDownloadRequest request =
                (UniProtKBDownloadRequest) this.converter.fromMessage(message);
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
            if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
                processH5Message(message, request, downloadJob, idsFile, jobId);
            } else {
                writeResult(request, idsFile, jobId, contentType);
                updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
            }
        }
    }

    private void processH5Message(Message message,
            UniProtKBDownloadRequest request, DownloadJob downloadJob, Path idsFile, String jobId) {
        try {
            Long totalHits = getSolrHits(request);
            Long maxAllowedHits = this.embeddingsQueueConfigProps.getMaxEntryCount();
            if (maxAllowedHits >= totalHits) {
                writeSolrResult(request, idsFile, jobId);
                sendMessageToEmbeddingsQueue(jobId);
                updateDownloadJob(message, downloadJob, JobStatus.UNFINISHED);
            } else {
                log.warn("Embeddings limit exceeded {}. Max allowed {}", totalHits, maxAllowedHits);
                updateDownloadJob(
                        downloadJob,
                        JobStatus.ABORTED,
                        String.format(H5_LIMIT_EXCEED_MSG, maxAllowedHits),
                        downloadJob.getRetried(),
                        null);
            }
        } catch (Exception ex) {
            logMessageAndDeleteFile(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    private Long getSolrHits(UniProtKBDownloadRequest request) {
        UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setSize(0);
        QueryResult<UniProtKBEntry> searchResults = service.search(searchRequest);
        Long totalHits = searchResults.getPage().getTotalElements();
        return totalHits;
    }

    private void writeResult(
            DownloadRequest request, Path idsFile, String jobId, MediaType contentType) {
        try {
            writeSolrResult(request, idsFile, jobId);
            StoreRequest storeRequest = service.buildStoreRequest(request);
            downloadResultWriter.writeResult(
                    request, idsFile, jobId, contentType, storeRequest, DATA_TYPE);
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

    private void sendMessageToEmbeddingsQueue(String jobId) {
        log.info(
                "Sending h5 message to embeddings queue for further processing for jobId {}",
                jobId);
        MessageProperties msgProps = new MessageProperties();
        msgProps.setHeader(JOB_ID_HEADER, jobId);
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
