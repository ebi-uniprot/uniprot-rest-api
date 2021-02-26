package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.UniRefIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@RestController
@RequestMapping(value = "/uniref/idmapping/")
public class UniRefIdMappingResultsController extends BasicSearchController<UniRefEntryPair> {

    private final UniRefIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniRefIdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            UniRefIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniRefEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIREF);
        this.idService = idService;
        this.cacheService = cacheService;
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniRefEntryPair>> getMappedEntries(
            @PathVariable String jobId,
            @Valid UniProtKBIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        IdMappingJob cachedJobResult =
                cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniRefEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest, cachedJobResult.getIdMappingResult());

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(UniRefEntryPair entity) {
        return entity.getTo().getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniRefEntryPair entity) {
        return Optional.empty();
    }
}
