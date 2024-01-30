package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingDownloadController.ID_MAPPING_DOWNLOAD_RESOURCE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;
import org.uniprot.api.idmapping.controller.validator.IdMappingDownloadRequestValidator;
import org.uniprot.api.idmapping.controller.validator.IdMappingDownloadRequestValidatorFactory;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.queue.IdMappingProducerMessageService;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.controller.BasicDownloadController;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.job.DownloadJobDetailResponse;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "downloadJob", description = "APIs related to idMapping download job")
@RestController
@RequestMapping(value = ID_MAPPING_DOWNLOAD_RESOURCE)
public class IdMappingDownloadController extends BasicDownloadController {
    static final String ID_MAPPING_DOWNLOAD_RESOURCE = IDMAPPING_PATH + "/download";

    private final IdMappingProducerMessageService messageService;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final DownloadJobRepository jobRepository;

    protected IdMappingDownloadController(
            IdMappingProducerMessageService messageService,
            IdMappingJobCacheService idMappingJobCacheService,
            DownloadJobRepository jobRepository,
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration) {
        super(asyncDownloadHeartBeatConfiguration);
        this.messageService = messageService;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Submit a idmapping download job.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema =
                                            @Schema(
                                                    implementation =
                                                            IdMappingDownloadRequest.class))
                        })
            })
    public ResponseEntity<JobSubmitResponse> submitDownloadJob(
            @Valid @ModelAttribute IdMappingDownloadRequestImpl request) {
        validateRequest(request);
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(new JobSubmitResponse(jobId));
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get the status of a idmapping download job.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JobStatus.class))
                        })
            })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "Unique identifier for idmapping job") @PathVariable
                    String jobId) {
        Optional<DownloadJob> optJob = jobRepository.findById(jobId);
        DownloadJob job = getAsyncDownloadJob(optJob, jobId);
        return getAsyncDownloadStatus(job);
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<DownloadJobDetailResponse> getDetails(
            @Parameter(description = "Unique identifier for idmapping job") @PathVariable
                    String jobId,
            HttpServletRequest servletRequest) {

        Optional<DownloadJob> optJob = this.jobRepository.findById(jobId);
        DownloadJob job = getAsyncDownloadJob(optJob, jobId);

        DownloadJobDetailResponse detailResponse = new DownloadJobDetailResponse();
        detailResponse.setFields(job.getFields());
        detailResponse.setFormat(job.getFormat());
        if (JobStatus.FINISHED == job.getStatus()) {
            detailResponse.setRedirectURL(
                    constructDownloadRedirectUrl(
                            job.getResultFile(), servletRequest.getRequestURL().toString()));
        } else if (JobStatus.ERROR == job.getStatus()) {
            ProblemPair error =
                    new ProblemPair(PredefinedAPIStatus.SERVER_ERROR.getCode(), job.getError());
            detailResponse.setErrors(List.of(error));
        }

        return ResponseEntity.ok(detailResponse);
    }

    private void validateRequest(IdMappingDownloadRequest request) {
        String jobId = request.getJobId();
        IdMappingJob idMappingJob = idMappingJobCacheService.getJobAsResource(jobId);
        if (idMappingJob.getJobStatus() != FINISHED) {
            throw new InvalidRequestException(
                    "IdMapping Job must be finished, before we start to download it.");
        }
        String type = idMappingJob.getIdMappingRequest().getTo();
        IdMappingDownloadRequestValidatorFactory validatorFactory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator validator = validatorFactory.create(type);
        validator.validate(request);
    }
}
