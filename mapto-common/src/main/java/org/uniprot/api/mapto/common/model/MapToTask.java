package org.uniprot.api.mapto.common.model;

import java.util.LinkedHashSet;
import java.util.List;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;

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
    private final Integer maxTargetIdCount;

    public MapToTask(
            MapToSearchService mapToSearchService,
            MapToJobService mapToJobService,
            MapToJob mapToJob,
            RetryPolicy<Object> retryPolicy,
            Integer maxIdMappingToIdsCount) {
        this.mapToSearchService = mapToSearchService;
        this.mapToJobService = mapToJobService;
        this.mapToJob = mapToJob;
        this.retryPolicy = retryPolicy;
        this.maxTargetIdCount = maxIdMappingToIdsCount;
    }

    @Override
    public void run() {
        String jobId = mapToJob.getJobId();
        try {
            mapToJobService.updateStatus(jobId, JobStatus.RUNNING);
            MapToSearchResult targetIdPage = getTargetIdPage(mapToJob, null);
            CursorPage page = targetIdPage.getPage();
            List<String> allTargetIds = getAllTargetIds(targetIdPage, page);
            validateTargetIdLimitAndProcess(jobId, allTargetIds, maxTargetIdCount);
        } catch (Exception e) {
            log.error("Job with id %s finished with error %s".formatted(jobId, e.getMessage()));
            mapToJobService.setErrors(
                    jobId,
                    new ProblemPair(PredefinedAPIStatus.SERVER_ERROR.getCode(), e.getMessage()));
        }
    }

    private void validateTargetIdLimitAndProcess(
            String jobId, List<String> allTargetIds, Integer targetIdLimit) {
        int totalTargetIds = allTargetIds.size();
        if (exceedsTargetIdLimit(targetIdLimit, totalTargetIds)) {
            mapToJobService.setErrors(
                    jobId,
                    new ProblemPair(
                            PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(),
                            "Number of target ids: %d exceeds the allowed limit: %d"
                                    .formatted(totalTargetIds, targetIdLimit)));
        } else {
            mapToJobService.setTargetIds(jobId, allTargetIds);
        }
    }

    private static boolean exceedsTargetIdLimit(Integer limit, int totalElements) {
        return limit != null && totalElements > limit;
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
