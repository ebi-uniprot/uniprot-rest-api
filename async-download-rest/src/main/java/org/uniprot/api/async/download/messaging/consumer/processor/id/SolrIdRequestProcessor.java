package org.uniprot.api.async.download.messaging.consumer.processor.id;

import java.util.Map;
import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class SolrIdRequestProcessor<
                T extends SolrStreamDownloadRequest, R extends DownloadJob>
        implements IdRequestProcessor<T> {
    protected static final String TOTAL_ENTRIES = "totalEntries";
    private final FileHandler downloadFileHandler;
    private final JobService<R> jobService;

    protected SolrIdRequestProcessor(FileHandler downloadFileHandler, JobService<R> jobService) {
        this.downloadFileHandler = downloadFileHandler;
        this.jobService = jobService;
    }

    @Override
    public void process(T request) {
        updateTotalEntries(request, getSolrHits(request));
        writeIdentifiers(request, streamIds(request));
    }

    protected void updateTotalEntries(T request, long totalEntries) {
        jobService.update(request.getDownloadJobId(), Map.of(TOTAL_ENTRIES, totalEntries));
    }

    protected abstract long getSolrHits(T downloadRequest);

    private void writeIdentifiers(T request, Stream<String> ids) {
        downloadFileHandler.writeIds(request.getDownloadJobId(), ids);
    }

    protected abstract Stream<String> streamIds(T downloadRequest);
}
