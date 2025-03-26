package org.uniprot.api.mapto.common.model;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.config.UniProtDataType;

import java.util.LinkedHashSet;
import java.util.List;

import static org.uniprot.api.mapto.common.search.MapToSearchService.checkTheResultLimits;

@EqualsAndHashCode
@Slf4j
public class MapToTask implements Runnable {
    private final MapToSearchService mapToSearchService;
    private final MapToJobService mapToJobService;
    private final MapToJob mapToJob;
    private final RetryPolicy<Object> retryPolicy;

    public MapToTask(MapToSearchService mapToSearchService, MapToJobService mapToJobService, MapToJob mapToJob, RetryPolicy<Object> retryPolicy) {
        this.mapToSearchService = mapToSearchService;
        this.mapToJobService = mapToJobService;
        this.mapToJob = mapToJob;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void run() {
        mapToJobService.updateStatus(mapToJob.getId(), JobStatus.RUNNING);

        MapToSearchResult targetIdPage = getTargetIdPage(mapToJob.getQuery(), mapToJob.getTargetDB(), null);
        CursorPage page = targetIdPage.getPage();
        checkTheResultLimits(page.getTotalElements());
        List<String> allTargetIds = getAllTargetIds(targetIdPage, page);

        mapToJobService.setTargetIds(mapToJob.getId(), allTargetIds);
    }

    private List<String> getAllTargetIds(MapToSearchResult targetIdPage, CursorPage page) {
        LinkedHashSet<String> allTargetIds = new LinkedHashSet<>(targetIdPage.getTargetIds());

        while (page.hasNextPage()) {
            targetIdPage = getTargetIdPage(mapToJob.getQuery(), mapToJob.getTargetDB(), page.getEncryptedNextCursor());
            page = targetIdPage.getPage();
            allTargetIds.addAll(targetIdPage.getTargetIds());
        }

        return allTargetIds.stream().toList();
    }

    private MapToSearchResult getTargetIdPage(String query, UniProtDataType targetDataType, String cursor) {
        return Failsafe.with(
                        retryPolicy.onRetry(
                                e ->
                                        log.warn(
                                                "Batch call to solr server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount())))
                .get(() -> mapToSearchService.getTargetIds(query, targetDataType, cursor));
    }
}
