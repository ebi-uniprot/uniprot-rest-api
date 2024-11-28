package org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.id;

import static org.uniprot.api.async.download.messaging.repository.JobFields.*;
import static org.uniprot.api.rest.download.model.JobStatus.*;

import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.processor.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.messaging.service.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UniProtKBSolrIdHD5RequestProcessor
        implements IdRequestProcessor<UniProtKBDownloadRequest> {
    protected static final String JOB_ID_HEADER = "jobId";
    private final UniProtKBJobService jobService;
    private final EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties;
    private final UniProtKBMessagingService messagingService;
    private final UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor;

    public UniProtKBSolrIdHD5RequestProcessor(
            UniProtKBFileHandler downloadFileHandler,
            UniProtKBJobService jobService,
            EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties,
            UniProtKBMessagingService messagingService,
            UniProtEntryService uniProtKBEntryService) {
        this.jobService = jobService;
        this.embeddingsQueueConfigProperties = embeddingsQueueConfigProperties;
        this.messagingService = messagingService;
        uniProtKBSolrIdRequestProcessor =
                new UniProtKBSolrIdRequestProcessor(
                        downloadFileHandler, jobService, uniProtKBEntryService);
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        long solrHits = uniProtKBSolrIdRequestProcessor.getSolrHits(request);
        long maxEntryCount = embeddingsQueueConfigProperties.getMaxEntryCount();

        if (solrHits <= maxEntryCount) {
            uniProtKBSolrIdRequestProcessor.process(request);
            sendMessageToEmbeddingsQueue(request.getDownloadJobId());
            jobService.update(request.getDownloadJobId(), Map.of(STATUS.getName(), UNFINISHED));
        } else {
            log.warn("Embeddings limit exceeded {}. Max allowed {}", solrHits, maxEntryCount);
            jobService.update(
                    request.getDownloadJobId(),
                    Map.of(
                            STATUS.getName(),
                            ABORTED,
                            "error",
                            "Embeddings Limit Exceeded. Embeddings download must be under %s entries. Current download: %s"
                                    .formatted(maxEntryCount, solrHits)));
        }
    }

    private void sendMessageToEmbeddingsQueue(String jobId) {
        log.info(
                "Sending h5 message to embeddings queue for further processing for jobId {}",
                jobId);
        MessageProperties msgProps = new MessageProperties();
        msgProps.setHeader(JOB_ID_HEADER, jobId);
        Message message = new Message(new byte[] {}, msgProps);
        messagingService.send(
                message,
                embeddingsQueueConfigProperties.getExchangeName(),
                embeddingsQueueConfigProperties.getRoutingKey());
        log.info(
                "Message with jobId {} sent to embeddings queue {}",
                jobId,
                this.embeddingsQueueConfigProperties.getQueueName());
    }
}
