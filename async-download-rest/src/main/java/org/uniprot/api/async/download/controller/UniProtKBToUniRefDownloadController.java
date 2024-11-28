package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.async.download.controller.UniProtKBToUniRefDownloadController.DOWNLOAD_RESOURCE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.producer.mapto.UniProtKBToUniRefProducerMessageService;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

import io.swagger.v3.oas.annotations.Operation;
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
    private final UniProtKBToUniRefProducerMessageService messageService;

    public UniProtKBToUniRefDownloadController(
            UniProtKBToUniRefProducerMessageService uniRefRabbitProducerMessageService,
            HeartbeatConfig heartbeatConfig) {
        super(heartbeatConfig);
        this.messageService = uniRefRabbitProducerMessageService;
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
}
