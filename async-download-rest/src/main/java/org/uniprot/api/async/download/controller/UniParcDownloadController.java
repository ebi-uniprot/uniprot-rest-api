package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniParcDownloadController.DOWNLOAD_RESOURCE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.producer.uniparc.UniParcProducerMessageService;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
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

@Tag(name = TAG_UNIPARC_JOB, description = TAG_UNIPARC_JOB_DESC)
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
public class UniParcDownloadController extends BasicDownloadController {

    static final String UNIPARC_RESOURCE = "/uniparc";
    static final String DOWNLOAD_RESOURCE = UNIPARC_RESOURCE + "/download";
    private final UniParcProducerMessageService messageService;
    private final UniParcJobService jobService;

    public UniParcDownloadController(
            UniParcProducerMessageService uniParcProducerMessageService,
            HeartbeatConfig heartbeatConfig,
            UniParcJobService jobService) {
        super(heartbeatConfig);
        this.messageService = uniParcProducerMessageService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = JOB_RUN_UNIPARC_OPERATION,
            description = JOB_RUN_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobSubmitResponse.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniParcDownloadRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_STATUS_UNIPARC_OPERATION,
            description = JOB_STATUS_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = JOB_ID_UNIPARC_DESCRIPTION) @PathVariable String jobId) {

        UniParcDownloadJob job = getDownloadJob(jobId);
        return getAsyncDownloadStatus(job);
    }

    private UniParcDownloadJob getDownloadJob(String jobId) {
        return this.jobService
                .find(jobId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(getDownloadJobNotExistMessage(jobId)));
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_DETAILS_UNIPARC_OPERATION,
            description = JOB_DETAILS_UNIPARC_OPERATION_DESC,
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
            @Parameter(description = JOB_ID_UNIPARC_DESCRIPTION) @PathVariable String jobId,
            HttpServletRequest servletRequest) {

        UniParcDownloadJob job = getDownloadJob(jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
