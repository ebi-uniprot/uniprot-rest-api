package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.uniparc.UniParcIdMappingSearchRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.UniParcIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@RestController
@RequestMapping(value = "/uniparc/idmapping/")
public class UniParcIdMappingController extends BasicSearchController<UniParcEntryPair> {

    private final UniParcIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniParcIdMappingController(
            ApplicationEventPublisher eventPublisher,
            UniParcIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniParcEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB);
        this.idService = idService;
        this.cacheService = cacheService;
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniParcEntryPair>> getMappedEntries(
            @Valid UniParcIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getJobAsResource(searchRequest.getJobId());

        QueryResult<UniParcEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest, cachedJobResult.getIdMappingResult());

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(UniParcEntryPair entity) {
        return entity.getTo().getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniParcEntryPair entity) {
        return Optional.empty();
    }
}
