package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController.DOWNLOAD_RESOURCE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.job.DownloadJobDetailResponse;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBDownloadRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

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
public class UniProtKBDownloadController extends BasicSearchController<UniProtKBEntry> {
    static final String DOWNLOAD_RESOURCE = UNIPROTKB_RESOURCE + "/download";
    private final ProducerMessageService messageService;
    private final DownloadJobRepository jobRepository;
    private final HashGenerator<DownloadRequest> hashGenerator;
    public static final String JOB_ID = "jobId";

    private static final String SALT_STR = "UNIPROT_DOWNLOAD_SALT";

    public UniProtKBDownloadController(
            ProducerMessageService messageService,
            DownloadJobRepository jobRepository,
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPROTKB,
                downloadGatekeeper);
        this.messageService = messageService;
        this.jobRepository = jobRepository;
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniProtKBDownloadRequest request,
            HttpServletRequest httpRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = this.hashGenerator.generateHash(request);
        messageHeader.setHeader(JOB_ID, jobId);

        if (Objects.isNull(request.getContentType())) {
            request.setContentType(APPLICATION_JSON_VALUE);
        }

        request.setLargeSolrStreamRestricted(false);

        this.messageService.sendMessage(request, messageHeader);
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
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @PathVariable String jobId, HttpServletRequest servletRequest) {
        DownloadJob job = getAsyncDownloadJob(this.jobRepository, jobId);
        return createAsyncDownloadStatus(job, servletRequest.getRequestURL().toString());
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<DownloadJobDetailResponse> getDetails(
            @PathVariable String jobId, HttpServletRequest servletRequest) {
        DownloadJob job = getAsyncDownloadJob(this.jobRepository, jobId);

        DownloadJobDetailResponse detailResponse = new DownloadJobDetailResponse();
        detailResponse.setQuery(job.getQuery());
        detailResponse.setFields(job.getFields());
        detailResponse.setSort(job.getSort());
        detailResponse.setContentType(job.getContentType());
        if (JobStatus.FINISHED == job.getStatus()) {
            detailResponse.setRedirectURL(
                    constructDownloadRedirectUrl(
                            job.getResultFile(), servletRequest.getRequestURL().toString()));
        } else if (JobStatus.ERROR == job.getStatus()) {
            ProblemPair error = new ProblemPair(EXCEPTION_CODE, job.getError());
            detailResponse.setErrors(List.of(error));
        }

        return ResponseEntity.ok(detailResponse);
    }

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniProtKBEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
