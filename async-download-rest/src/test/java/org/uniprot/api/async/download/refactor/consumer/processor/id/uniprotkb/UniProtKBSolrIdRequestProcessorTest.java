package org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.id.SolrIdRequestProcessorTest;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniProtKBSolrIdRequestProcessorTest extends SolrIdRequestProcessorTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private static final String QUERY = "uniprotKBQuery";
    private static final String INCLUDE_ISOFORMS = "false";
    @Mock
    private QueryResult<UniProtKBEntry> searchResults;
    @Mock
    protected UniProtEntryService uniProtEntryService;
    @Mock
    private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock
    private UniProtKBJobService uniProtKBJobService;
    @Mock
    private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock
    private Page page;


    @BeforeEach
    void setUp() {
        asyncDownloadFileHandler = uniProtKBAsyncDownloadFileHandler;
        jobService = uniProtKBJobService;
        downloadRequest = uniProtKBDownloadRequest;
        requestProcessor = new UniProtKBSolrIdRequestProcessor(uniProtKBAsyncDownloadFileHandler, uniProtKBJobService, uniProtEntryService);
        mockUniProtKB();
    }

    protected void mockUniProtKB() {
        mockRequest();
        mockStream();
        mockSolrHits();
    }

    protected void mockSolrHits() {
        when(uniProtEntryService.search(argThat(sr -> QUERY.equals(((UniProtKBSearchRequest) sr).getQuery()) && Boolean.parseBoolean(INCLUDE_ISOFORMS) == ((UniProtKBSearchRequest) sr).isIncludeIsoform() && sr.getSize() == 0)))
                .thenReturn(searchResults);
        when(searchResults.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(SOLR_HITS);
    }

    private void mockStream() {
        when(uniProtEntryService.streamIdsForDownload(downloadRequest)).thenReturn(idStream);
    }

    protected void mockRequest() {
        when(downloadRequest.getQuery()).thenReturn(QUERY);
        when(downloadRequest.getIncludeIsoform()).thenReturn(INCLUDE_ISOFORMS);
    }
}
