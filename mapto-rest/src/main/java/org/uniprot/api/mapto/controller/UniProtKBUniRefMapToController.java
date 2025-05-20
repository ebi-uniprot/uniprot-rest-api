package org.uniprot.api.mapto.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.common.request.JobDetailResponse;
import org.uniprot.api.mapto.common.service.MapToJobSubmissionService;
import org.uniprot.api.mapto.common.service.UniRefMapToTargetService;
import org.uniprot.api.mapto.request.UniProtKBMapToSearchRequest;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;

@RestController
@RequestMapping(UniProtKBUniRefMapToController.RESOURCE_PATH)
public class UniProtKBUniRefMapToController extends BasicSearchController<UniRefEntryLight> {
    private static final String DATA_TYPE = "uniref";
    public static final String UNIPROTKB_UNIREF = "/uniprotkb/uniref";
    public static final String MAPTO = "/mapto";
    public static final String RESOURCE_PATH = MAPTO + UNIPROTKB_UNIREF;
    private final MapToJobSubmissionService mapToJobSubmissionService;
    private final MapToRequestConverter mapToRequestConverter;
    private final UniRefMapToTargetService uniRefMapToTargetService;

    public UniProtKBUniRefMapToController(
            MapToJobSubmissionService mapToJobSubmissionService,
            MapToRequestConverter mapToRequestConverter,
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            UniRefMapToTargetService uniRefMapToTargetService,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIREF,
                downloadGatekeeper);
        this.mapToJobSubmissionService = mapToJobSubmissionService;
        this.mapToRequestConverter = mapToRequestConverter;
        this.uniRefMapToTargetService = uniRefMapToTargetService;
    }

    @PostMapping(value = "/run", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<JobSubmitResponse> submitMapToJob(
            @Valid @ModelAttribute UniProtKBMapToSearchRequest request) {
        request.setTo(UniProtDataType.UNIREF);
        JobSubmitResponse jobSubmitResponse =
                mapToJobSubmissionService.submit(mapToRequestConverter.convert(request));
        return ResponseEntity.ok(jobSubmitResponse);
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        JobStatusResponse jobStatus = mapToJobSubmissionService.getJobStatus(jobId);
        return ResponseEntity.ok(jobStatus);
    }

    @GetMapping(
            value = "/details/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobDetailResponse> getJobDetails(
            @PathVariable String jobId, HttpServletRequest request) {
        JobDetailResponse jobDetail =
                mapToJobSubmissionService.getJobDetails(
                        jobId, request.getRequestURL().toString(), UNIPROTKB_UNIREF);
        return ResponseEntity.ok(jobDetail);
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> getMapToEntries(
            @PathVariable String jobId,
            @Valid @ModelAttribute UniRefSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (!mapToJobSubmissionService.isJobFinished(jobId)) {
            throw new ResourceNotFoundException("{search.not.found}");
        }

        return getSearchResponse(
                uniRefMapToTargetService.getMappedEntries(jobId, searchRequest),
                searchRequest.getFields(),
                false,
                request,
                response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefEntryLight>>>
            streamMapToEntries(
                    @PathVariable String jobId,
                    @Valid @ModelAttribute UniRefStreamRequest streamRequest,
                    HttpServletRequest request) {
        JobStatusResponse status = mapToJobSubmissionService.getJobStatus(jobId);
        if (status.getJobStatus() != JobStatus.FINISHED) {
            throw new ResourceNotFoundException("{search.not.found}");
        }

        this.uniRefMapToTargetService.validateMappedIdsEnrichmentLimit(
                Math.toIntExact(status.getTotalEntries()));

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        MediaType contentType = getAcceptHeader(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            uniRefMapToTargetService.streamRdf(
                                    jobId, streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> uniRefMapToTargetService.streamEntries(jobId, streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(UniRefEntryLight entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniRefEntryLight entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
