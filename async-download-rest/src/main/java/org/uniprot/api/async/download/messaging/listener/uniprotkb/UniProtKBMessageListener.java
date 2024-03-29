package org.uniprot.api.async.download.messaging.listener.uniprotkb;

import java.nio.file.Path;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.AbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.MessageListenerException;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service
@Slf4j
public class UniProtKBMessageListener extends AbstractMessageListener implements MessageListener {
    public static final String H5_LIMIT_EXCEED_MSG =
            "Embeddings Limit Exceeded. Embeddings download must be under %s entries. Current download: %s";
    private static final String UNIPROTKB_DATA_TYPE = "uniprotkb";
    private final UniProtEntryService service;
    private final EmbeddingsQueueConfigProperties embeddingsQueueConfigProps;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties,
            UniProtKBAsyncDownloadQueueConfigProperties queueConfigProperties,
            UniProtKBDownloadJobRepository jobRepository,
            UniProtKBDownloadResultWriter uniProtKBDownloadResultWriter,
            UniProtKBRabbitTemplate uniProtKBRabbitTemplate,
            EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties,
            UniProtKBHeartbeatProducer heartbeatProducer,
            UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler) {
        super(
                converter,
                uniProtKBDownloadConfigProperties,
                jobRepository,
                uniProtKBDownloadResultWriter,
                uniProtKBRabbitTemplate,
                heartbeatProducer,
                uniProtKBAsyncDownloadFileHandler,
                queueConfigProperties);
        this.service = service;
        this.embeddingsQueueConfigProps = embeddingsQueueConfigProperties;
    }

    @Override
    protected void updateStatusAndWriteResult(
            Message message, DownloadJob downloadJob, DownloadRequest request, Path idsFile) {
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
        if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            processH5Message(message, (UniProtKBDownloadRequest) request, downloadJob, idsFile);
        } else {
            Long totalHits = getSolrHits((UniProtKBDownloadRequest) request);
            updateTotalEntries(downloadJob, totalHits);
            writeResult(request, downloadJob, idsFile, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
        }
    }

    private void processH5Message(
            Message message,
            UniProtKBDownloadRequest request,
            DownloadJob downloadJob,
            Path idsFile) {
        String jobId = downloadJob.getId();
        try {
            Long totalHits = getSolrHits(request);
            updateTotalEntries(downloadJob, totalHits);
            Long maxAllowedHits = this.embeddingsQueueConfigProps.getMaxEntryCount();
            if (maxAllowedHits >= totalHits) {
                writeSolrResult(request, downloadJob, idsFile);
                sendMessageToEmbeddingsQueue(jobId);
                updateDownloadJob(message, downloadJob, JobStatus.UNFINISHED);
            } else {
                log.warn("Embeddings limit exceeded {}. Max allowed {}", totalHits, maxAllowedHits);
                updateDownloadJob(
                        downloadJob,
                        JobStatus.ABORTED,
                        String.format(H5_LIMIT_EXCEED_MSG, maxAllowedHits, totalHits),
                        downloadJob.getRetried(),
                        null);
            }
        } catch (Exception ex) {
            logMessage(ex, jobId);
            throw new MessageListenerException(ex);
        }
    }

    private Long getSolrHits(UniProtKBDownloadRequest request) {
        UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setIncludeIsoform(request.getIncludeIsoform());
        searchRequest.setSize(0);
        QueryResult<UniProtKBEntry> searchResults = service.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    protected StoreRequest getStoreRequest(DownloadRequest request) {
        return service.buildStoreRequest(request);
    }

    @Override
    public Stream<String> streamIds(DownloadRequest request) {
        return service.streamIdsForDownload(request);
    }

    @Override
    protected String getDataType() {
        return UNIPROTKB_DATA_TYPE;
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
