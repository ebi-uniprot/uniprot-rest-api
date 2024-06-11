package org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb.UniProtKBSolrIdHD5RequestProcessor.STATUS;
import static org.uniprot.api.rest.download.model.JobStatus.ABORTED;
import static org.uniprot.api.rest.download.model.JobStatus.UNFINISHED;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.mq.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBSolrIdHD5RequestProcessorTest {
    protected static final String ID = "someId";
    private static final String EMBEDDINGS_EXCHANGE = "embeddingsExchange";
    private static final String EMBEDDINGS_ROUTING_KEY = "embeddingsRoutingKey";
    private static final String QUERY = "uniprotKBQuery";
    private static final String INCLUDE_ISOFORMS = "false";
    protected static final long SOLR_HITS = 98L;
    private static final long MAX_ENTRY_COUNT = 1000L;
    @Mock private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private UniProtEntryService uniProtEntryService;
    @Mock private EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties;
    @Mock private UniProtKBMessagingService uniProtKBMessagingService;
    @Mock protected Stream<String> idStream;
    private UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor;
    @Mock private QueryResult<UniProtKBEntry> searchResults;
    @Mock private Page page;

    @BeforeEach
    void setUp() {
        uniProtKBSolrIdHD5RequestProcessor =
                new UniProtKBSolrIdHD5RequestProcessor(
                        uniProtKBAsyncDownloadFileHandler,
                        uniProtKBJobService,
                        embeddingsQueueConfigProperties,
                        uniProtKBMessagingService,
                        uniProtEntryService);
    }

    @Test
    void process() {
        mock(SOLR_HITS);
        mockProcessing();

        uniProtKBSolrIdHD5RequestProcessor.process(uniProtKBDownloadRequest);

        verify(uniProtKBMessagingService)
                .send(
                        argThat(msg -> ID.equals(msg.getMessageProperties().getHeader("jobId"))),
                        eq(EMBEDDINGS_EXCHANGE),
                        eq(EMBEDDINGS_ROUTING_KEY));
        verify(uniProtKBJobService).update(ID, Map.of(STATUS, UNFINISHED));
    }

    private void mock(long solrHits) {
        when(uniProtKBDownloadRequest.getId()).thenReturn(ID);
        when(embeddingsQueueConfigProperties.getMaxEntryCount()).thenReturn(MAX_ENTRY_COUNT);
        when(uniProtKBDownloadRequest.getQuery()).thenReturn(QUERY);
        when(uniProtKBDownloadRequest.getIncludeIsoform()).thenReturn(INCLUDE_ISOFORMS);
        when(uniProtEntryService.search(
                        argThat(
                                sr ->
                                        QUERY.equals(((UniProtKBSearchRequest) sr).getQuery())
                                                && Boolean.parseBoolean(INCLUDE_ISOFORMS)
                                                        == ((UniProtKBSearchRequest) sr)
                                                                .isIncludeIsoform()
                                                && sr.getSize() == 0)))
                .thenReturn(searchResults);
        when(searchResults.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(solrHits);
    }

    private void mockProcessing() {
        when(embeddingsQueueConfigProperties.getExchangeName()).thenReturn(EMBEDDINGS_EXCHANGE);
        when(embeddingsQueueConfigProperties.getRoutingKey()).thenReturn(EMBEDDINGS_ROUTING_KEY);
        when(uniProtEntryService.streamIdsForDownload(uniProtKBDownloadRequest))
                .thenReturn(idStream);
    }

    @Test
    void process_whenNoOfEntriesExceeded() {
        long exceededCount = 5000L;
        mock(exceededCount);

        uniProtKBSolrIdHD5RequestProcessor.process(uniProtKBDownloadRequest);

        verify(uniProtKBJobService)
                .update(
                        ID,
                        Map.of(
                                STATUS,
                                ABORTED,
                                "error",
                                "Embeddings Limit Exceeded. Embeddings download must be under %d entries. Current download: %d"
                                        .formatted(MAX_ENTRY_COUNT, exceededCount)));
        verify(uniProtKBMessagingService, never()).send(any());
        verify(uniProtKBAsyncDownloadFileHandler, never()).writeIds(any(), any());
    }
}
