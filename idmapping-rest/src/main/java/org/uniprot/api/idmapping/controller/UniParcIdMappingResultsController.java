package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.uniparc.UniParcIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.uniparc.UniParcIdMappingStreamRequest;
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
@RequestMapping(value = IdMappingJobController.IDMAPPING_PATH + "/uniparc/")
public class UniParcIdMappingResultsController extends BasicSearchController<UniParcEntryPair> {

    private final UniParcIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniParcIdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            UniParcIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniParcEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
        this.idService = idService;
        this.cacheService = cacheService;
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {
                APPLICATION_JSON_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniParcEntryPair>> getMappedEntries(
            @PathVariable String jobId,
            @Valid UniParcIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniParcEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest, cachedJobResult.getIdMappingResult());

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {RDF_MEDIA_TYPE_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<UniParcEntryPair>>>
            streamMappedEntries(
                    @PathVariable String jobId,
                    @Valid UniParcIdMappingStreamRequest streamRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {

        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);
        MediaType contentType = getAcceptHeader(request);

        if (contentType.equals(RDF_MEDIA_TYPE)) {
            Stream<String> result =
                    this.idService.streamRDF(streamRequest, cachedJobResult.getIdMappingResult());
            return super.streamRDF(result, streamRequest, contentType, request);
        }
        return null; // FIXME
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
