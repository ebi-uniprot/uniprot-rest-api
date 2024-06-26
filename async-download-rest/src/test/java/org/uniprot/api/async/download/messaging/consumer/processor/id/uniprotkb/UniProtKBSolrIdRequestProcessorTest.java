package org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.id.SolrIdRequestProcessorTest;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBSolrIdRequestProcessorTest
        extends SolrIdRequestProcessorTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private static final String QUERY = "uniprotKBQuery";
    private static final String INCLUDE_ISOFORMS = "false";
    @Mock private QueryResult<UniProtKBEntry> searchResults;
    @Mock protected UniProtEntryService uniProtEntryService;
    @Mock private UniProtKBFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private Page page;

    @BeforeEach
    void setUp() {
        fileHandler = uniProtKBAsyncDownloadFileHandler;
        jobService = uniProtKBJobService;
        downloadRequest = uniProtKBDownloadRequest;
        requestProcessor =
                new UniProtKBSolrIdRequestProcessor(
                        uniProtKBAsyncDownloadFileHandler,
                        uniProtKBJobService,
                        uniProtEntryService);
        when(downloadRequest.getQuery()).thenReturn(QUERY);
        when(downloadRequest.getIncludeIsoform()).thenReturn(INCLUDE_ISOFORMS);
        when(uniProtEntryService.streamIdsForDownload(downloadRequest)).thenReturn(idStream);
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
        when(page.getTotalElements()).thenReturn(SOLR_HITS);
    }
}
