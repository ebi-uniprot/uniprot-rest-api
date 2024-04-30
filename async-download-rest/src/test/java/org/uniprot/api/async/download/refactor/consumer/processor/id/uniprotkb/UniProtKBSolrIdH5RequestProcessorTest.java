package org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdH5RequestProcessor.STATUS;
import static org.uniprot.api.rest.download.model.JobStatus.ABORTED;
import static org.uniprot.api.rest.download.model.JobStatus.UNFINISHED;

@ExtendWith(MockitoExtension.class)
class UniProtKBSolrIdH5RequestProcessorTest extends UniProtKBSolrIdRequestProcessorTest {
    public static final String EMBEDDINGS_EXCHANGE = "embeddingsExchange";
    public static final String EMBEDDINGS_ROUTING_KEY = "embeddingsRoutingKey";
    public static final long MAX_ENTRY_COUNT = 1000L;
    @Mock
    private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock
    private UniProtKBJobService uniProtKBJobService;
    @Mock
    private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock
    private EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties;
    @Mock
    private UniProtKBMessagingService uniProtKBMessagingService;


    @BeforeEach
    void setUp() {
        asyncDownloadFileHandler = uniProtKBAsyncDownloadFileHandler;
        jobService = uniProtKBJobService;
        downloadRequest = uniProtKBDownloadRequest;
        requestProcessor = new UniProtKBSolrIdH5RequestProcessor(uniProtKBAsyncDownloadFileHandler, uniProtKBJobService, embeddingsQueueConfigProperties,
                uniProtKBMessagingService, uniProtEntryService);
    }

    @Override
    protected void doAdditionalMocks() {
        mockUniProtKB();
        when(embeddingsQueueConfigProperties.getExchangeName()).thenReturn(EMBEDDINGS_EXCHANGE);
        when(embeddingsQueueConfigProperties.getRoutingKey()).thenReturn(EMBEDDINGS_ROUTING_KEY);
        when(embeddingsQueueConfigProperties.getMaxEntryCount()).thenReturn(MAX_ENTRY_COUNT);
    }

    @Test
    void process_whenNoOfEntriesExceeded() {
        mock();
        mockRequest();
        mockSolrHits();
        when(embeddingsQueueConfigProperties.getMaxEntryCount()).thenReturn(50L);

        requestProcessor.process(downloadRequest);

        verify(jobService).update(JOB_ID, Map.of(STATUS, ABORTED, "error",  "Embeddings Limit Exceeded. Embeddings download must be under 50 entries. Current download: 98"));
        verify(uniProtKBMessagingService, never()).send(any());
        verify(uniProtKBAsyncDownloadFileHandler, never()).writeIds(any(), any());
    }

    @Override
    protected void doAdditionalVerifications() {
        verify(uniProtKBMessagingService).send(argThat(msg -> JOB_ID.equals(msg.getMessageProperties().getHeader("jobId"))), eq(EMBEDDINGS_EXCHANGE), eq(EMBEDDINGS_ROUTING_KEY));
        verify(jobService).update(JOB_ID, Map.of(STATUS, UNFINISHED));
    }
}