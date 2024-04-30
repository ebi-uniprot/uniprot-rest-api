package org.uniprot.api.async.download.refactor.consumer.processor.id;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.SolrStreamDownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.refactor.consumer.processor.id.SolrIdRequestProcessor.TOTAL_ENTRIES;

public abstract  class SolrIdRequestProcessorTest<T extends SolrStreamDownloadRequest, R extends DownloadJob> {
    protected static final long SOLR_HITS = 98L;
    protected static final String JOB_ID = "someJobId";
    @Mock
    protected Stream<String> idStream;
    protected AsyncDownloadFileHandler asyncDownloadFileHandler;
    protected JobService<R> jobService;
    protected T downloadRequest;
    protected SolrIdRequestProcessor<T, R> requestProcessor;

    @Test
    void process() {
        mock();
        doAdditionalMocks();

        requestProcessor.process(downloadRequest);

        verify(jobService).update(JOB_ID, Map.of(TOTAL_ENTRIES, SOLR_HITS));
        verify(asyncDownloadFileHandler).writeIds(JOB_ID, idStream);
        doAdditionalVerifications();
    }

    protected void mock() {
        when(downloadRequest.getJobId()).thenReturn(JOB_ID);
    }

    protected void doAdditionalMocks() {

    }

    protected void doAdditionalVerifications() {
    }
}