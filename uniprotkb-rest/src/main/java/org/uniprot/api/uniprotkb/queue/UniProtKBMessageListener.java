package org.uniprot.api.uniprotkb.queue;

import java.nio.file.Path;
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
import org.uniprot.api.rest.download.queue.*;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
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
    public static final String H5_LIMIT_EXCEED_MSG =
            "Embeddings Limit Exceeded. Embeddings download must be under %s entries. Current download: %s";
    private static final String UNIPROTKB_DATA_TYPE = "uniprotkb";
    private final UniProtEntryService service;
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
                converter,
                downloadConfigProperties,
                asyncDownloadQueueConfigProperties,
                jobRepository,
                downloadResultWriter,
                rabbitTemplate);
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
            processH5Message(
                    message, (UniProtKBDownloadRequest) request, downloadJob, idsFile, jobId);
        } else {
            writeResult(request, idsFile, jobId, contentType);
            updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
        }
    }

    private void processH5Message(
            Message message,
            UniProtKBDownloadRequest request,
            DownloadJob downloadJob,
            Path idsFile,
            String jobId) {
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
                        String.format(H5_LIMIT_EXCEED_MSG, maxAllowedHits, totalHits),
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
    protected Stream<String> streamIds(DownloadRequest request) {
        return service.streamIds(request);
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
