package org.uniprot.api.async.download.messaging.consumer.processor.mapto.to;

import java.util.Iterator;
import java.util.List;

import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class UniRefBatchEntryIterable extends BatchIterable<Long> {
    private final UniRefEntryLightService uniRefEntryLightService;
    private final RetryPolicy<Object> retryPolicy;

    public UniRefBatchEntryIterable(
            Iterator<String> sourceIterator,
            UniRefEntryLightService uniRefEntryLightService,
            RetryPolicy<Object> retryPolicy,
            int batchSize) {
        super(sourceIterator, batchSize);
        this.uniRefEntryLightService = uniRefEntryLightService;
        this.retryPolicy = retryPolicy;
    }

    @Override
    protected List<Long> convertBatch(List<String> batch) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to Solr server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> getTotalNoOfResults(batch));
    }

    private List<Long> getTotalNoOfResults(List<String> batch) {
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery(getQuery(batch));
        searchRequest.setSize(0);
        return List.of(uniRefEntryLightService.search(searchRequest).getPage().getTotalElements());
    }

    private String getQuery(List<String> batch) {
        return "uniprot_id: (" + String.join(" OR ", batch) + ")";
    }
}
