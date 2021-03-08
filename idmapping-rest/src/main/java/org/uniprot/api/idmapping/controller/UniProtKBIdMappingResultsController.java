package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.GFF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

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
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.UniProtKBIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * @author sahmad
 * @created 17/02/2021
 */
@Slf4j
@RestController
@RequestMapping(value = IdMappingJobController.IDMAPPING_PATH + "/uniprotkb/")
public class UniProtKBIdMappingResultsController extends BasicSearchController<UniProtKBEntryPair> {

    private final UniProtKBIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniProtKBIdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            UniProtKBIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB);
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
                FF_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniProtKBEntryPair>> getMappedEntries(
            @PathVariable String jobId,
            @Valid UniProtKBIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniProtKBEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest, cachedJobResult.getIdMappingResult());
        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {RDF_MEDIA_TYPE_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<UniProtKBEntryPair>>>
            streamMappedEntries(
                    @PathVariable String jobId,
                    @Valid UniProtKBIdMappingStreamRequest streamRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);
        MediaType contentType = getAcceptHeader(request);
        if (contentType.equals(RDF_MEDIA_TYPE)) {
            Stream<String> result =
                    this.idService.streamRDF(streamRequest, cachedJobResult.getIdMappingResult());
            return super.streamRDF(result, streamRequest, contentType, request);
        } else {
            Stream<UniProtKBEntryPair> result =
                    this.idService.streamEntries(
                            streamRequest, cachedJobResult.getIdMappingResult());
            return super.stream(result, streamRequest, contentType, request);
        }
    }

    @Override
    protected String getEntityId(UniProtKBEntryPair entity) {
        return entity.getTo().getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniProtKBEntryPair entity) {
        return Optional.empty();
    }
}
