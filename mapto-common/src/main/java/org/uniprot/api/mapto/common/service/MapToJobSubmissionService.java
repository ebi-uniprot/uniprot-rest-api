package org.uniprot.api.mapto.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.request.JobDetailResponse;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.model.MapToTask;
import org.uniprot.api.mapto.common.search.MapToSearchFacade;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

import net.jodah.failsafe.RetryPolicy;

@Service
public class MapToJobSubmissionService {
    private static final String RESULTS_SUBPATH = "results/";
    public static final String MAPTO = "/mapto";
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final MapToHashGenerator hashGenerator;
    private final MapToJobService mapToJobService;
    private final MapToSearchFacade mapToSearchFacade;
    private final RetryPolicy<Object> retryPolicy;
    private final Integer maxTargetIdCount;

    public MapToJobSubmissionService(
            ThreadPoolTaskExecutor jobTaskExecutor,
            MapToHashGenerator hashGenerator,
            MapToJobService mapToJobService,
            MapToSearchFacade mapToSearchFacade,
            RetryPolicy<Object> retryPolicy,
            @Value("${mapping.max.to.ids.count:#{null}}") Integer maxTargetIdCount) {
        this.jobTaskExecutor = jobTaskExecutor;
        this.hashGenerator = hashGenerator;
        this.mapToJobService = mapToJobService;
        this.mapToSearchFacade = mapToSearchFacade;
        this.retryPolicy = retryPolicy;
        this.maxTargetIdCount = maxTargetIdCount;
    }

    public JobSubmitResponse submit(MapToJobRequest mapToJobRequest) {
        String jobId = hashGenerator.generateHash(mapToJobRequest);
        if (!mapToJobService.mapToJobExists(jobId)) {
            MapToJob mapToJob = mapToJobService.createMapToJob(jobId, mapToJobRequest);
            MapToSearchService mapToSearchService =
                    mapToSearchFacade.getMapToSearchService(mapToJobRequest.getSource());
            MapToTask mapToTask =
                    new MapToTask(
                            mapToSearchService,
                            mapToJobService,
                            mapToJob,
                            retryPolicy,
                            maxTargetIdCount);
            jobTaskExecutor.execute(mapToTask);
        } else {
            mapToJobService.updateUpdated(jobId);
        }
        return new JobSubmitResponse(jobId);
    }

    public JobStatusResponse getJobStatus(String jobId) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        return new JobStatusResponse(
                mapToJob.getStatus(),
                null,
                mapToJob.getErrors(),
                mapToJob.getCreated(),
                mapToJob.getTargetIds() != null ? (long) mapToJob.getTargetIds().size() : null,
                null,
                mapToJob.getUpdated());
    }

    public JobDetailResponse getJobDetails(String jobId, String requestUrl, String mappingType) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);

        JobDetailResponse jobDetailResponse = new JobDetailResponse();
        jobDetailResponse.setFrom(mapToJob.getSourceDB().name());
        jobDetailResponse.setTo(mapToJob.getTargetDB().name());
        jobDetailResponse.setQuery(mapToJob.getQuery());
        jobDetailResponse.setIncludeIsoform(mapToJob.getExtraParams().get("includeIsoform"));
        if (JobStatus.FINISHED == mapToJob.getStatus()) {
            jobDetailResponse.setRedirectURL(
                    getRedirectPathToResults(jobId, requestUrl, mappingType));
        }
        return jobDetailResponse;
    }

    public boolean isJobFinished(String jobId) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        return mapToJob.getStatus() == JobStatus.FINISHED;
    }

    private String getRedirectPathToResults(String jobId, String requestUrl, String mappingType) {
        String requestUrlBase = extractRequestBase(requestUrl);
        return requestUrlBase + mappingType + "/" + RESULTS_SUBPATH + jobId;
    }

    private String extractRequestBase(String requestUrl) {
        int endOfIdMappingPath = requestUrl.indexOf(MAPTO) + MAPTO.length();
        return requestUrl.substring(0, endOfIdMappingPath).replaceFirst("http://", "https://");
    }
}
