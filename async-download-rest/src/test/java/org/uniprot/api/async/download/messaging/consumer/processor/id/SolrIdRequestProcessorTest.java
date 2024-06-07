package org.uniprot.api.async.download.messaging.consumer.processor.id;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.id.SolrIdRequestProcessor.TOTAL_ENTRIES;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class SolrIdRequestProcessorTest<
        T extends SolrStreamDownloadRequest, R extends DownloadJob> {
    protected static final long SOLR_HITS = 98L;
    protected static final String ID = "someId";
    @Mock protected Stream<String> idStream;
    protected AsyncDownloadFileHandler asyncDownloadFileHandler;
    protected JobService<R> jobService;
    protected T downloadRequest;
    protected SolrIdRequestProcessor<T, R> requestProcessor;

    @Test
    void process() {
        when(downloadRequest.getId()).thenReturn(ID);

        requestProcessor.process(downloadRequest);

        verify(jobService).update(ID, Map.of(TOTAL_ENTRIES, SOLR_HITS));
        verify(asyncDownloadFileHandler).writeIds(ID, idStream);
    }
}
