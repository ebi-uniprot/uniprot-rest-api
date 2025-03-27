package org.uniprot.api.mapto.common.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.model.MapToTask;
import org.uniprot.api.mapto.common.search.MapToSearchFacade;
import org.uniprot.api.mapto.common.search.MapToSearchService;

import net.jodah.failsafe.RetryPolicy;

@Component
public class MapToJobSubmissionService {
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final MapToHashGenerator hashGenerator;
    private final MapToJobService mapToJobService;
    private final MapToSearchFacade mapToSearchFacade;
    private final RetryPolicy<Object> retryPolicy;

    public MapToJobSubmissionService(
            ThreadPoolTaskExecutor jobTaskExecutor,
            MapToHashGenerator hashGenerator,
            MapToJobService mapToJobService,
            MapToSearchFacade mapToSearchFacade,
            RetryPolicy<Object> retryPolicy) {
        this.jobTaskExecutor = jobTaskExecutor;
        this.hashGenerator = hashGenerator;
        this.mapToJobService = mapToJobService;
        this.mapToSearchFacade = mapToSearchFacade;
        this.retryPolicy = retryPolicy;
    }

    public void submit(MapToJobRequest mapToJobRequest) {
        String jobId = hashGenerator.generateHash(mapToJobRequest);
        MapToJob mapToJob = mapToJobService.createMapToJob(jobId, mapToJobRequest);
        MapToSearchService mapToSearchService =
                mapToSearchFacade.getMapToSearchService(mapToJobRequest.getSource());
        MapToTask mapToTask =
                new MapToTask(mapToSearchService, mapToJobService, mapToJob, retryPolicy);
        jobTaskExecutor.execute(mapToTask);
    }
}
