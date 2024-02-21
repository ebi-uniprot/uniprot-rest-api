package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.common.service.IdMappingJobService.IDMAPPING_PATH;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobDetailResponse;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Tag(name = TAG_IDMAPPING, description = TAG_IDMAPPING_DESC)
@RestController
@RequestMapping(IDMAPPING_PATH)
public class IdMappingJobController {

    private final IdMappingJobService idMappingJobService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public IdMappingJobController(
            IdMappingJobService idMappingJobService, IdMappingJobCacheService cacheService) {
        this.idMappingJobService = idMappingJobService;
        this.cacheService = cacheService;
    }

    @PostMapping(
            value = "/run",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = RUN_IDMAPPING_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobSubmitResponse.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute IdMappingJobRequest request) {
        JobSubmitResponse response = this.idMappingJobService.submitJob(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = STATUS_IDMAPPING_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatusResponse.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getStatus(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            HttpServletRequest servletRequest) {
        return createStatus(
                cacheService.getJobAsResource(jobId), servletRequest.getRequestURL().toString());
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = DETAILS_IDMAPPING_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobDetailResponse.class))
                        })
            })
    public ResponseEntity<JobDetailResponse> getDetails(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            HttpServletRequest servletRequest) {
        IdMappingJob job = cacheService.getJobAsResource(jobId);
        IdMappingJobRequest jobRequest = job.getIdMappingRequest();

        JobDetailResponse detailResponse = new JobDetailResponse();
        detailResponse.setFrom(jobRequest.getFrom());
        detailResponse.setTo(jobRequest.getTo());
        detailResponse.setIds(jobRequest.getIds());
        detailResponse.setTaxId(jobRequest.getTaxId());
        if (JobStatus.FINISHED == job.getJobStatus()) {
            detailResponse.setRedirectURL(
                    idMappingJobService.getRedirectPathToResults(
                            job, servletRequest.getRequestURL().toString()));
            detailResponse.setWarnings(getWarnings(job));
        } else if (JobStatus.ERROR == job.getJobStatus()) {
            detailResponse.setErrors(getLimitExceedError(job));
        }

        return ResponseEntity.ok(detailResponse);
    }

    private ResponseEntity<JobStatusResponse> createStatus(IdMappingJob job, String url) {
        ResponseEntity<JobStatusResponse> response;
        switch (job.getJobStatus()) {
            case NEW:
            case RUNNING:
                response = ResponseEntity.ok(new JobStatusResponse(job.getJobStatus()));
                break;
            case FINISHED:
                String redirectUrl = idMappingJobService.getRedirectPathToResults(job, url);
                response =
                        ResponseEntity.status(HttpStatus.SEE_OTHER)
                                .header(HttpHeaders.LOCATION, redirectUrl)
                                .body(new JobStatusResponse(JobStatus.FINISHED, getWarnings(job)));
                break;
            default:
                if (!getLimitExceedError(job).isEmpty()) {
                    // limit exceed error
                    response =
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(new JobStatusResponse(getLimitExceedError(job)));

                } else {
                    response =
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(new JobStatusResponse(JobStatus.ERROR));
                }
                break;
        }

        return response;
    }

    private List<ProblemPair> getWarnings(IdMappingJob job) {
        List<ProblemPair> warnings = new ArrayList<>();
        if (Objects.nonNull(job.getIdMappingResult())) {
            warnings = job.getIdMappingResult().getWarnings();
        }
        return warnings;
    }

    private List<ProblemPair> getLimitExceedError(IdMappingJob job) {
        List<ProblemPair> errors = new ArrayList<>();
        if (Objects.nonNull(job.getIdMappingResult())) {
            errors = job.getIdMappingResult().getErrors();
        }
        return errors;
    }
}
