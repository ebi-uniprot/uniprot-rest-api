package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniProtKBToUniRefDownloadController.DOWNLOAD_RESOURCE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.producer.mapto.UniProtKBMapToProducerMessageService;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
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
 * @author supun
 * @created 14/08/2023
 */
@Tag(
        name = TAG_UNIPROTKB_TO_UNIREF_MAP_TO_JOB,
        description = TAG_UNIPROTKB_TO_UNIREF_MAP_TO_JOB_DESC)
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
public class UniProtKBToUniRefDownloadController extends BasicDownloadController {

    static final String UNIPROTKB_TO_UNIREF_MAP_RESOURCE = "/mapto/uniprotkb/uniref";
    static final String DOWNLOAD_RESOURCE = UNIPROTKB_TO_UNIREF_MAP_RESOURCE + "/download";
    private final UniProtKBMapToProducerMessageService messageService;
    private final MapToJobService jobService;

    public UniProtKBToUniRefDownloadController(
            UniProtKBMapToProducerMessageService uniRefRabbitProducerMessageService,
            HeartbeatConfig heartbeatConfig,
            MapToJobService jobService) {
        super(heartbeatConfig);
        this.messageService = uniRefRabbitProducerMessageService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = JOB_RUN_UNIPROTKB_TO_UNIREF_MAP_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobSubmitResponse.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniProtKBToUniRefDownloadRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_STATUS_UNIPROTKB_TO_UNIREF_MAP_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = JOB_ID_UNIPROTKB_TO_UNIREF_MAP_DESCRIPTION) @PathVariable
                    String jobId) {

        MapToDownloadJob job = getDownloadJob(jobId);
        return getAsyncDownloadStatus(job);
    }

    private MapToDownloadJob getDownloadJob(String jobId) {
        return this.jobService
                .find(jobId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(getDownloadJobNotExistMessage(jobId)));
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = JOB_DETAILS_UNIPROTKB_TO_UNIREF_MAP_OPERATION,
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
            @Parameter(description = JOB_ID_UNIPROTKB_TO_UNIREF_MAP_DESCRIPTION) @PathVariable
                    String jobId,
            HttpServletRequest servletRequest) {

        MapToDownloadJob job = getDownloadJob(jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
