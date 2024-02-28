package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniRefDownloadController.DOWNLOAD_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.async.download.model.DownloadJob;
import org.uniprot.api.async.download.model.DownloadJobDetailResponse;
import org.uniprot.api.async.download.queue.common.ProducerMessageService;
import org.uniprot.api.async.download.queue.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author tibrahim
 * @created 14/08/2023
 */
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
public class UniRefDownloadController extends BasicDownloadController {

    static final String UNIREF_RESOURCE = "/uniref";
    static final String DOWNLOAD_RESOURCE = UNIREF_RESOURCE + "/download";
    private final ProducerMessageService messageService;
    private final DownloadJobRepository jobRepository;

    public UniRefDownloadController(
            @Qualifier("uniRef") ProducerMessageService messageService,
            DownloadJobRepository jobRepository,
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration) {
        super(asyncDownloadHeartBeatConfiguration);
        this.messageService = messageService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniRefDownloadRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get the status of a job.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        Optional<DownloadJob> optJob = jobRepository.findById(jobId);
        DownloadJob job = getAsyncDownloadJob(optJob, jobId);
        return getAsyncDownloadStatus(job);
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<DownloadJobDetailResponse> getDetails(
            @PathVariable String jobId, HttpServletRequest servletRequest) {

        Optional<DownloadJob> optJob = this.jobRepository.findById(jobId);
        DownloadJob job = getAsyncDownloadJob(optJob, jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
