package org.uniprot.api.async.download.refactor.consumer.processor.id;

import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.SolrStreamDownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import java.util.Map;
import java.util.stream.Stream;

public abstract class SolrIdRequestProcessor<T extends SolrStreamDownloadRequest, R extends DownloadJob> implements IdRequestProcessor<T> {
    protected static final String JOB_ID_HEADER = "jobId";
    public static final String TOTAL_ENTRIES = "totalEntries";
    private final AsyncDownloadFileHandler downloadFileHandler;
    private final JobService<R> jobService;

    protected SolrIdRequestProcessor(AsyncDownloadFileHandler downloadFileHandler, JobService<R> jobService) {
        this.downloadFileHandler = downloadFileHandler;
        this.jobService = jobService;
    }

    @Override
    public void process(T request) {
        updateTotalEntries(request, getSolrHits(request));
        writeIdentifiers(request, streamIds(request));
    }

    protected void updateTotalEntries(T request, long totalEntries) {
        jobService.update(request.getJobId(), Map.of(TOTAL_ENTRIES,totalEntries));
    }

    protected abstract long getSolrHits(T downloadRequest);

    private void writeIdentifiers(T request, Stream<String> ids) {
        downloadFileHandler.writeIds(request.getJobId(), ids);
    }

    protected abstract Stream<String> streamIds(T downloadRequest);
}
