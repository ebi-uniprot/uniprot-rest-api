package org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.refactor.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

import java.util.Map;

import static org.uniprot.api.rest.download.model.JobStatus.*;

@Slf4j
@Component
public class UniProtKBSolrIdHD5RequestProcessor implements IdRequestProcessor<UniProtKBDownloadRequest> {
    protected static final String STATUS = "status";
    protected static final String JOB_ID_HEADER = "jobId";
    private final UniProtKBJobService jobService;
    private final EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties;
    private final UniProtKBMessagingService messagingService;
    private final UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor;

    public UniProtKBSolrIdHD5RequestProcessor(UniProtKBAsyncDownloadFileHandler downloadFileHandler, UniProtKBJobService jobService, EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties, UniProtKBMessagingService messagingService, UniProtEntryService uniProtKBEntryService) {
        this.jobService = jobService;
        this.embeddingsQueueConfigProperties = embeddingsQueueConfigProperties;
        this.messagingService = messagingService;
        uniProtKBSolrIdRequestProcessor = new UniProtKBSolrIdRequestProcessor(downloadFileHandler, jobService, uniProtKBEntryService);
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        long solrHits = uniProtKBSolrIdRequestProcessor.getSolrHits(request);
        long maxEntryCount = embeddingsQueueConfigProperties.getMaxEntryCount();

        if (solrHits <= maxEntryCount) {
            uniProtKBSolrIdRequestProcessor.process(request);
            sendMessageToEmbeddingsQueue(request.getJobId());
            jobService.update(request.getJobId(), Map.of(STATUS, UNFINISHED));
        } else {
            log.warn("Embeddings limit exceeded {}. Max allowed {}", solrHits, maxEntryCount);
            jobService.update(request.getJobId(), Map.of(STATUS, ABORTED, "error",
                    "Embeddings Limit Exceeded. Embeddings download must be under %s entries. Current download: %s".formatted(maxEntryCount, solrHits)));
        }
    }

    private void sendMessageToEmbeddingsQueue(String jobId) {
        log.info("Sending h5 message to embeddings queue for further processing for jobId {}", jobId);
        MessageProperties msgProps = new MessageProperties();
        msgProps.setHeader(JOB_ID_HEADER, jobId);
        Message message = new Message(new byte[]{}, msgProps);
        messagingService.send(message, embeddingsQueueConfigProperties.getExchangeName(), embeddingsQueueConfigProperties.getRoutingKey());
        log.info("Message with jobId {} sent to embeddings queue {}", jobId, this.embeddingsQueueConfigProperties.getQueueName());
    }
}