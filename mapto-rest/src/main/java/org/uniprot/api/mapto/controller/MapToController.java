package org.uniprot.api.mapto.controller;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.mapto.common.model.MapToEntryId;
import org.uniprot.api.mapto.common.service.MapToJobSubmissionService;
import org.uniprot.api.mapto.common.service.UniRefMapToTargetService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.IDMAPPING_PIR;

@RestController
@RequestMapping(UniProtKBUniRefMapToController.MAPTO)
public class MapToController extends BasicSearchController<MapToEntryId> {
    private final MapToJobSubmissionService mapToJobSubmissionService;

    public MapToController(MapToJobSubmissionService mapToJobSubmissionService, ApplicationEventPublisher eventPublisher,
                           MessageConverterContextFactory<MapToEntryId> converterContextFactory,
                           UniRefMapToTargetService uniRefMapToTargetService,
                           ThreadPoolTaskExecutor downloadTaskExecutor,
                           Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                IDMAPPING_PIR,
                downloadGatekeeper);
        this.mapToJobSubmissionService = mapToJobSubmissionService;
    }


    @GetMapping(
            value = "/stream/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<MapToEntryId>>> streamMapToIds(@PathVariable String jobId, HttpServletRequest request) {
        if (!mapToJobSubmissionService.isJobFinished(jobId)) {
            throw new ResourceNotFoundException("{search.not.found}");
        }
        return super.stream(
                () -> mapToJobSubmissionService.getMapToEntryIds(jobId).stream(),
                null,
                getAcceptHeader(request),
                request);
    }

    @Override
    protected String getEntityId(MapToEntryId entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(MapToEntryId entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
