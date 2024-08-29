package org.uniprot.api.async.download.messaging.consumer.processor.id.uniparc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.id.SolrIdRequestProcessorTest;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.uniparc.common.service.light.UniParcLightEntryService;
import org.uniprot.api.uniparc.common.service.request.UniParcSearchRequest;
import org.uniprot.core.uniparc.UniParcEntryLight;

@ExtendWith(MockitoExtension.class)
class UniParcSolrIdRequestProcessorTest
        extends SolrIdRequestProcessorTest<UniParcDownloadRequest, UniParcDownloadJob> {
    private static final String QUERY = "uniParcQuery";
    @Mock private QueryResult<UniParcEntryLight> searchResults;
    @Mock private UniParcLightEntryService uniParcLightEntryService;
    @Mock private UniParcFileHandler uniParcAsyncDownloadFileHandler;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private Page page;

    @BeforeEach
    void setUp() {
        fileHandler = uniParcAsyncDownloadFileHandler;
        jobService = uniParcJobService;
        downloadRequest = uniParcDownloadRequest;
        requestProcessor =
                new UniParcSolrIdRequestProcessor(
                        uniParcAsyncDownloadFileHandler,
                        uniParcJobService,
                        uniParcLightEntryService);
        when(downloadRequest.getQuery()).thenReturn(QUERY);
        when(uniParcLightEntryService.streamIdsForDownload(downloadRequest)).thenReturn(idStream);
        when(uniParcLightEntryService.search(
                        argThat(
                                sr ->
                                        QUERY.equals(((UniParcSearchRequest) sr).getQuery())
                                                && sr.getSize() == 0)))
                .thenReturn(searchResults);
        when(searchResults.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(SOLR_HITS);
    }
}
