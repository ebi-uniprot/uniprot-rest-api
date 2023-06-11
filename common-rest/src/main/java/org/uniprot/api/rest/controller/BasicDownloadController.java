package org.uniprot.api.rest.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;

public abstract class BasicDownloadController {

    protected BasicDownloadController() {}

    protected ResponseEntity<JobStatusResponse> getAsyncDownloadStatus(DownloadJob job) {
        ResponseEntity<JobStatusResponse> response;
        switch (job.getStatus()) {
            case NEW:
            case RUNNING:
            case FINISHED:
                response = ResponseEntity.ok(new JobStatusResponse(job.getStatus()));
                break;
            case PROCESSING:
            case UNFINISHED:
                response = ResponseEntity.ok(new JobStatusResponse(JobStatus.RUNNING));
                break;
            case ABORTED:
                ProblemPair abortedError = new ProblemPair(PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(), job.getError());
                response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JobStatusResponse(JobStatus.ABORTED, List.of(), List.of(abortedError)));
                break;
            default:
                ProblemPair error =
                        new ProblemPair(PredefinedAPIStatus.SERVER_ERROR.getCode(), job.getError());
                response =
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new JobStatusResponse(List.of(error)));
                break;
        }

        return response;
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
}
