package org.uniprot.api.mapto.common.model;

import java.util.LinkedHashSet;
import java.util.List;

import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;
import org.uniprot.api.rest.download.model.JobStatus;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@EqualsAndHashCode
@Slf4j
public class MapToTask implements Runnable {
    private final MapToSearchService mapToSearchService;
    private final MapToJobService mapToJobService;
    private final MapToJob mapToJob;
    private final RetryPolicy<Object> retryPolicy;

    public MapToTask(
            MapToSearchService mapToSearchService,
            MapToJobService mapToJobService,
            MapToJob mapToJob,
            RetryPolicy<Object> retryPolicy) {
        this.mapToSearchService = mapToSearchService;
        this.mapToJobService = mapToJobService;
        this.mapToJob = mapToJob;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void run() {
        String jobId = mapToJob.getId();
        mapToJobService.updateStatus(jobId, JobStatus.RUNNING);

        MapToSearchResult targetIdPage = getTargetIdPage(mapToJob, null);
        CursorPage page = targetIdPage.getPage();
        List<String> allTargetIds = getAllTargetIds(targetIdPage, page);
        String error = mapToSearchService.validateTargetLimit(Long.valueOf(allTargetIds.size()));
        // TODO handle error when solr is down or any exception is throwb.
        //  we should set the status to error with error message.
        // we should not set the whole stacktrace
        if (error != null) {
            mapToJobService.setErrors(jobId, error);
        } else {
            mapToJobService.setTargetIds(jobId, allTargetIds);
        }
    }

    private List<String> getAllTargetIds(MapToSearchResult targetIdPage, CursorPage page) {
        LinkedHashSet<String> allTargetIds = new LinkedHashSet<>(targetIdPage.getTargetIds());

        while (page.hasNextPage()) {
            targetIdPage = getTargetIdPage(mapToJob, page.getEncryptedNextCursor());
            page = targetIdPage.getPage();
            allTargetIds.addAll(targetIdPage.getTargetIds());
        }

        List<String> ids = allTargetIds.stream().toList();
        log.info("Total number of target ids are {}", ids.size());
        return ids;
    }

    private MapToSearchResult getTargetIdPage(MapToJob mapToJob, String cursor) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to solr server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> mapToSearchService.getTargetIds(mapToJob, cursor));
    }
}
