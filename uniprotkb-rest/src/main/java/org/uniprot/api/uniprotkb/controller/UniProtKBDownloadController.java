package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController.DOWNLOAD_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
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

    public static final String SALT_STR = "UNIPROT_DOWNLOAD_SALT"; // TODO Parametrized it

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
        // TODO make it a bean without new
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
    }

    @GetMapping(
            value = "/run",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            }) // TODO make it post to be consistent with idmapping job
    public ResponseEntity<String> submitJob(
            @ModelAttribute UniProtKBStreamRequest streamRequest, HttpServletRequest httpRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = this.hashGenerator.generateHash(streamRequest);
        messageHeader.setHeader(JOB_ID, jobId);
        messageHeader.setHeader(CONTENT_TYPE, getAcceptHeader(httpRequest));
        this.messageService.sendMessage(streamRequest, messageHeader);
        return ResponseEntity.ok(jobId);
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
    public ResponseEntity<String> getJobStatus(
            @PathVariable String jobId, HttpServletRequest servletRequest) {
        Optional<DownloadJob> optJob = this.jobRepository.findById(jobId);
        JobStatus status =
                optJob.orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "jobId" + jobId + "doesn't exist"))
                        .getStatus();
        return ResponseEntity.ok(status.name());
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
