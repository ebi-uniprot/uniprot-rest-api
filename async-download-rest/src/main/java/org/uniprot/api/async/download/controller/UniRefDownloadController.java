package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniRefDownloadController.DOWNLOAD_RESOURCE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.producer.uniref.UniRefProducerMessageService;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author tibrahim
 * @created 14/08/2023
 */
@Tag(name = TAG_UNIREF_JOB, description = TAG_UNIREF_JOB_DESC)
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
public class UniRefDownloadController extends BasicDownloadController {

    static final String UNIREF_RESOURCE = "/uniref";
    static final String DOWNLOAD_RESOURCE = UNIREF_RESOURCE + "/download";
    private final UniRefProducerMessageService messageService;
    private final UniRefJobService jobService;

    public UniRefDownloadController(
            UniRefProducerMessageService uniRefRabbitProducerMessageService,
            HeartbeatConfig heartbeatConfig,
            UniRefJobService jobService) {
        super(heartbeatConfig);
        this.messageService = uniRefRabbitProducerMessageService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = JOB_RUN_UNIREF_OPERATION,
            description = JOB_RUN_UNIREF_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobSubmitResponse.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniRefDownloadRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_STATUS_UNIREF_OPERATION,
            description = JOB_STATUS_UNIREF_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = JOB_ID_UNIREF_DESCRIPTION) @PathVariable String jobId) {

        UniRefDownloadJob job = getDownloadJob(jobId);
        return getAsyncDownloadStatus(job);
    }

    private UniRefDownloadJob getDownloadJob(String jobId) {
        return this.jobService
                .find(jobId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(getDownloadJobNotExistMessage(jobId)));
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_DETAILS_UNIREF_OPERATION,
            description = JOB_DETAILS_UNIREF_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema =
                                            @Schema(
                                                    implementation =
                                                            DownloadJobDetailResponse.class))
                        })
            })
    public ResponseEntity<DownloadJobDetailResponse> getDetails(
            @Parameter(description = JOB_ID_UNIREF_DESCRIPTION) @PathVariable String jobId,
            HttpServletRequest servletRequest) {

        UniRefDownloadJob job = getDownloadJob(jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
