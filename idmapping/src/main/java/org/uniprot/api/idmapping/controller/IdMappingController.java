package org.uniprot.api.idmapping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.response.JobStatus.FINISHED;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.IDMAPPING_PIR;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@RestController
@Validated
@RequestMapping(value = IdMappingController.IDMAPPING_RESOURCE)
public class IdMappingController extends BasicSearchController<IdMappingStringPair> {
    static final String IDMAPPING_RESOURCE = "/idmapping";
    private final IdMappingPIRService idMappingService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public IdMappingController(
            ApplicationEventPublisher eventPublisher,
            IdMappingPIRService idMappingService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<IdMappingStringPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, IDMAPPING_PIR);
        this.idMappingService = idMappingService;
        this.cacheService = cacheService;
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<IdMappingStringPair>> results(
            @PathVariable String jobId,
            @Valid IdMappingPageRequest pageRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (cacheService.exists(jobId)) {
            IdMappingJob mappingJob = cacheService.get(jobId);
            JobStatus jobStatus = mappingJob.getJobStatus();
            if (jobStatus == FINISHED) {
                QueryResult<IdMappingStringPair> queryResult =
                        idMappingService.queryResultPage(
                                pageRequest, mappingJob.getIdMappingResult());
                return super.getSearchResponse(queryResult, null, request, response);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping(
            value = "/stream",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<IdMappingStringPair>> stream(
            @Valid @ModelAttribute IdMappingBasicRequest mappingRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingResult idMappingResult = pirIdMapping(mappingRequest);
        QueryResult<IdMappingStringPair> queryResult =
                idMappingService.queryResultAll(idMappingResult);
        return super.getSearchResponse(queryResult, null, request, response);
    }

    @Override
    protected String getEntityId(IdMappingStringPair entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(IdMappingStringPair entity) {
        return Optional.empty();
    }

    private IdMappingResult pirIdMapping(IdMappingBasicRequest request) {
        IdMappingBasicRequest pirRequest = new IdMappingBasicRequest();
        pirRequest.setFrom(request.getFrom());
        pirRequest.setTo(request.getTo());
        pirRequest.setIds(request.getIds());
        pirRequest.setTaxId(request.getTaxId());

        return idMappingService.mapIds(pirRequest);
    }
}
