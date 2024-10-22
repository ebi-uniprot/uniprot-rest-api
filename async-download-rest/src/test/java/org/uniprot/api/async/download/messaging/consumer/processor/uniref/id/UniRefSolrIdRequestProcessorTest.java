package org.uniprot.api.async.download.messaging.consumer.processor.uniref.id;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdRequestProcessorTest;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
class UniRefSolrIdRequestProcessorTest
        extends SolrIdRequestProcessorTest<UniRefDownloadRequest, UniRefDownloadJob> {
    private static final String QUERY = "uniRefQuery";
    @Mock private QueryResult<UniRefEntryLight> searchResults;
    @Mock private UniRefEntryLightService uniRefEntryLightService;
    @Mock private UniRefFileHandler uniRefAsyncDownloadFileHandler;
    @Mock private UniRefJobService uniRefJobService;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private Page page;

    @BeforeEach
    void setUp() {
        fileHandler = uniRefAsyncDownloadFileHandler;
        jobService = uniRefJobService;
        downloadRequest = uniRefDownloadRequest;
        requestProcessor =
                new UniRefSolrIdRequestProcessor(
                        uniRefAsyncDownloadFileHandler, uniRefJobService, uniRefEntryLightService);
        when(downloadRequest.getQuery()).thenReturn(QUERY);
        when(uniRefEntryLightService.streamIdsForDownload(downloadRequest)).thenReturn(idStream);
        when(uniRefEntryLightService.search(
                        argThat(
                                sr ->
                                        QUERY.equals(((UniRefSearchRequest) sr).getQuery())
                                                && sr.getSize() == 0)))
                .thenReturn(searchResults);
        when(searchResults.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(SOLR_HITS);
    }
}
