package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.IDMAPPING_PIR;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@RestController
@Validated
@Slf4j
@RequestMapping(value = IdMappingJobController.IDMAPPING_PATH)
public class IdMappingResultsController extends BasicSearchController<IdMappingStringPair> {
    private final IdMappingPIRService idMappingService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public IdMappingResultsController(
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
        IdMappingJob completedJob = cacheService.getCompletedJobAsResource(jobId);

        Stopwatch stopwatch = Stopwatch.createStarted();
        QueryResult<IdMappingStringPair> queryResult =
                idMappingService.queryResultPage(pageRequest, completedJob.getIdMappingResult());
        stopwatch.stop();

        log.debug(
                "[idmapping/results/{}] response took {} seconds (id count={})",
                jobId,
                stopwatch.elapsed(TimeUnit.SECONDS),
                completedJob.getIdMappingRequest().getIds().split(",").length);
        return super.getSearchResponse(queryResult, null, request, response);
    }

    @GetMapping(
            value = "/stream/{jobId}",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<IdMappingStringPair>> streamResults(
            @PathVariable String jobId, HttpServletRequest request, HttpServletResponse response) {
        IdMappingJob completedJob = cacheService.getCompletedJobAsResource(jobId);
        QueryResult<IdMappingStringPair> queryResult =
                idMappingService.queryResultAll(completedJob.getIdMappingResult());
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
}
