package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController.DOWNLOAD_RESOURCE;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
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
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
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
    private final HashGenerator<StreamRequest> hashGenerator;
    public static final String JOB_ID = "jobId";
    public static final String CONTENT_TYPE = "content-type";

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

    @PostMapping(value = "/run",
            produces = {
                    TSV_MEDIA_TYPE_VALUE,
                    FF_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE,
                    APPLICATION_XML_VALUE,
                    APPLICATION_JSON_VALUE,
                    FASTA_MEDIA_TYPE_VALUE,
                    GFF_MEDIA_TYPE_VALUE,
                    RDF_MEDIA_TYPE_VALUE
                    })
    public ResponseEntity<JobSubmitResponse> submitJob(
            @Valid @ModelAttribute UniProtKBStreamRequest streamRequest,
            HttpServletRequest httpRequest) {
        MessageProperties messageHeader = new MessageProperties();
        MediaType contentType = getAcceptHeader(httpRequest);
        String jobId = this.hashGenerator.generateHash(streamRequest, contentType);
        messageHeader.setHeader(JOB_ID, jobId);
        messageHeader.setHeader(CONTENT_TYPE, contentType);
        this.messageService.sendMessage(streamRequest, messageHeader);
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
        if (JobStatus.FINISHED == job.getStatus()) {
            detailResponse.setRedirectURL(
                    constructDownloadRedirectUrl(
                            job.getResultFile(), servletRequest.getRequestURL().toString()));
        } else if (JobStatus.ERROR == job.getStatus()) {
            ProblemPair error = new ProblemPair(0, job.getError());
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
