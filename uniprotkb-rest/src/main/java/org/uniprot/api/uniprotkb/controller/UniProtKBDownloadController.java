package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController.DOWNLOAD_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.rest.controller.BasicDownloadController;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.job.DownloadJobDetailResponse;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.validation.CustomConstraintGroupSequence;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBDownloadRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author sahmad
 * @created 21/11/2022
 */
@RestController
@RequestMapping(value = DOWNLOAD_RESOURCE)
@Tag(name = "UniProtKB job", description = "UniProtKB asynchronous download jobs are different from \"normal\" downloads offered via stream. " +
        "First, a separate file must be generated for download. " +
        "Much like <tt>IDMapping</tt> services at UniProt, this file generation request can be submitted via the <tt>run</tt> post request, " +
        "which will return a job id. This id can be used to monitor the progress of the job via the <tt>status</tt> endpoint. " +
        "When the file generation job is completed, the <tt>status</tt> endpoint will redirect to the downloadable zip file.")
public class UniProtKBDownloadController extends BasicDownloadController {
    static final String DOWNLOAD_RESOURCE = UNIPROTKB_RESOURCE + "/download";
    private final ProducerMessageService messageService;
    private final DownloadJobRepository jobRepository;

    public UniProtKBDownloadController(
            ProducerMessageService messageService, DownloadJobRepository jobRepository) {
        this.messageService = messageService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Submit UniProtKB asynchronous download job.",
            responses = {
                    @ApiResponse(
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = JobSubmitResponse.class))
                            })})
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
            summary = "Get progress of UniProtKB asynchronous download job.",
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
    @Operation(
            summary = "Get details of UniProtKB asynchronous download job.",
            responses = {
                    @ApiResponse(
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = DownloadJobDetailResponse.class))
                            })
            })
    public ResponseEntity<DownloadJobDetailResponse> getDetails(
            @PathVariable String jobId, HttpServletRequest servletRequest) {

        Optional<DownloadJob> optJob = this.jobRepository.findById(jobId);
        DownloadJob job = getAsyncDownloadJob(optJob, jobId);
        String requestURL = servletRequest.getRequestURL().toString();

        return getDownloadJobDetails(requestURL, job);
    }
}
