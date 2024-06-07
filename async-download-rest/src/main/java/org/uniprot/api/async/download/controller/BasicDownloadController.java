package org.uniprot.api.async.download.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;

public abstract class BasicDownloadController {
    private final HeartbeatConfig heartbeatConfig;

    protected BasicDownloadController(HeartbeatConfig heartbeatConfig) {
        this.heartbeatConfig = heartbeatConfig;
    }

    protected ResponseEntity<JobStatusResponse> getAsyncDownloadStatus(DownloadJob job) {
        ResponseEntity<JobStatusResponse> response;
        switch (job.getStatus()) {
            case NEW:
            case RUNNING:
            case FINISHED:
                response =
                        ResponseEntity.ok(
                                new JobStatusResponse(
                                        job.getStatus(),
                                        job.getCreated(),
                                        job.getTotalEntries(),
                                        getProcessedEntries(job),
                                        job.getUpdated()));
                break;
            case PROCESSING:
            case UNFINISHED:
                response =
                        ResponseEntity.ok(
                                new JobStatusResponse(
                                        JobStatus.RUNNING,
                                        job.getCreated(),
                                        job.getTotalEntries(),
                                        getProcessedEntries(job),
                                        job.getUpdated()));
                break;
            case ABORTED:
                ProblemPair abortedError =
                        new ProblemPair(
                                PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(), job.getError());
                response =
                        ResponseEntity.ok(
                                new JobStatusResponse(
                                        JobStatus.ABORTED,
                                        List.of(),
                                        List.of(abortedError),
                                        job.getCreated(),
                                        job.getTotalEntries(),
                                        getProcessedEntries(job),
                                        job.getUpdated()));
                break;
            default:
                ProblemPair error =
                        new ProblemPair(PredefinedAPIStatus.SERVER_ERROR.getCode(), job.getError());
                response =
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(
                                        new JobStatusResponse(
                                                List.of(error),
                                                job.getCreated(),
                                                job.getTotalEntries(),
                                                getProcessedEntries(job),
                                                job.getUpdated()));
                break;
        }

        return response;
    }

    private Long getProcessedEntries(DownloadJob job) {
        return heartbeatConfig.isEnabled() ? job.getProcessedEntries() : null;
    }

    protected String constructDownloadRedirectUrl(String resultFile, String url) {
        String requestBaseUrl = extractRequestBaseUrl(url);
        return requestBaseUrl + "results/" + resultFile;
    }

    protected DownloadJob getAsyncDownloadJob(Optional<DownloadJob> optJob, String jobId) {
        return optJob.orElseThrow(
                () -> new ResourceNotFoundException("jobId " + jobId + " doesn't exist"));
    }

    private String extractRequestBaseUrl(String url) {
        int index = url.indexOf("status");
        return index == -1
                ? url.substring(0, url.indexOf("details")).replaceFirst("http://", "https://")
                : url.substring(0, index).replaceFirst("http://", "https://");
    }

    protected ResponseEntity<DownloadJobDetailResponse> getDownloadJobDetails(
            String requestURL, DownloadJob job) {
        DownloadJobDetailResponse detailResponse = new DownloadJobDetailResponse();
        detailResponse.setQuery(job.getQuery());
        detailResponse.setFields(job.getFields());
        detailResponse.setSort(job.getSort());
        detailResponse.setFormat(job.getFormat());
        if (JobStatus.FINISHED == job.getStatus()) {
            detailResponse.setRedirectURL(
                    constructDownloadRedirectUrl(job.getResultFile(), requestURL));
        } else if (JobStatus.ERROR == job.getStatus()) {
            ProblemPair error =
                    new ProblemPair(PredefinedAPIStatus.SERVER_ERROR.getCode(), job.getError());
            detailResponse.setErrors(List.of(error));
        } else if (JobStatus.ABORTED == job.getStatus()) {
            ProblemPair error =
                    new ProblemPair(
                            PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(), job.getError());
            detailResponse.setErrors(List.of(error));
        }

        return ResponseEntity.ok(detailResponse);
    }
}
