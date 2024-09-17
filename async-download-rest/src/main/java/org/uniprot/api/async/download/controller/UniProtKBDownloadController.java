package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniProtKBDownloadController.DOWNLOAD_RESOURCE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.producer.uniprotkb.UniProtKBProducerMessageService;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.validation.CustomConstraintGroupSequence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 21/11/2022
 */
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
@Tag(name = TAG_UNIPROTKB_JOB, description = TAG_UNIPROTKB_JOB_DESC)
public class UniProtKBDownloadController extends BasicDownloadController {
    static final String DOWNLOAD_RESOURCE = "/uniprotkb/download";
    private final UniProtKBProducerMessageService messageService;
    private final UniProtKBJobService jobService;

    public UniProtKBDownloadController(
            UniProtKBProducerMessageService messagingService,
            HeartbeatConfig heartbeatConfig,
            UniProtKBJobService jobService) {
        super(heartbeatConfig);
        this.messageService = messagingService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = JOB_RUN_UNIPROTKB_OPERATION,
            description = JOB_RUN_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobSubmitResponse.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Validated(CustomConstraintGroupSequence.class) @ModelAttribute
                    UniProtKBDownloadRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_STATUS_UNIPROTKB_OPERATION,
            description = JOB_STATUS_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = JOB_ID_DESCRIPTION) @PathVariable String jobId) {
        UniProtKBDownloadJob job = getDownloadJob(jobId);
        return getAsyncDownloadStatus(job);
    }

    private UniProtKBDownloadJob getDownloadJob(String jobId) {
        return this.jobService
                .find(jobId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(getDownloadJobNotExistMessage(jobId)));
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_DETAILS_UNIPROTKB_OPERATION,
            description = JOB_DETAILS_UNIPROTKB_OPERATION_DESC,
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
            @Parameter(description = JOB_ID_DESCRIPTION) @PathVariable String jobId,
            HttpServletRequest servletRequest) {
        UniProtKBDownloadJob job = getDownloadJob(jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
