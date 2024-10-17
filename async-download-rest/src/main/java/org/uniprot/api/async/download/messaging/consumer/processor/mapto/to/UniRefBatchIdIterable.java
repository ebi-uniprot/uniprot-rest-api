package org.uniprot.api.async.download.messaging.consumer.processor.mapto.to;

import java.util.Iterator;
import java.util.List;

import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class UniRefBatchIdIterable extends BatchIterable<String> {
    private final UniRefEntryLightService uniRefEntryLightService;
    private final RetryPolicy<Object> retryPolicy;

    public UniRefBatchIdIterable(
            Iterator<String> sourceIterator,
            UniRefEntryLightService uniRefEntryLightService,
            RetryPolicy<Object> retryPolicy,
            int batchSize) {
        super(sourceIterator, batchSize);
        this.uniRefEntryLightService = uniRefEntryLightService;
        this.retryPolicy = retryPolicy;
    }

    @Override
    protected List<String> convertBatch(List<String> batch) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to Solr server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> getStreamResults(batch));
    }

    private List<String> getStreamResults(List<String> batch) {
        UniRefDownloadRequest uniRefDownloadRequest = new UniRefDownloadRequest();
        uniRefDownloadRequest.setQuery(getQuery(batch));
        return uniRefEntryLightService.streamIdsForDownload(uniRefDownloadRequest).toList();
    }

    private String getQuery(List<String> batch) {
        return "uniprot_id: (" + String.join(" OR ", batch) + ")";
    }
}
