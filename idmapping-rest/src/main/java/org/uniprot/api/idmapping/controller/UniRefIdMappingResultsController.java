package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.uniref.UniRefIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.uniref.UniRefIdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.UniRefIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Tag(name = "results", description = "APIs to get result of the submitted job.")
@RestController
@RequestMapping(
        value =
                IdMappingJobController.IDMAPPING_PATH
                        + "/"
                        + UniRefIdMappingResultsController.UNIREF_ID_MAPPING_PATH)
public class UniRefIdMappingResultsController extends BasicSearchController<UniRefEntryPair> {
    public static final String UNIREF_ID_MAPPING_PATH = "uniref";
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
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Search result of UniRef cluster (or clusters) by a submitted job id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniRefEntryPair
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryPair>> getMappedEntries(
            @PathVariable String jobId,
            @Valid @ModelAttribute UniRefIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniRefEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest, cachedJobResult.getIdMappingResult());

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Stream an UniRef cluster (or clusters) retrieved by a submitted job id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniRefEntryPair
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefEntryPair>>>
            streamMappedEntries(
                    @PathVariable String jobId,
                    @Valid @ModelAttribute UniRefIdMappingStreamRequest streamRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {

        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);
        MediaType contentType = getAcceptHeader(request);

        if (contentType.equals(RDF_MEDIA_TYPE)) {
            Stream<String> result =
                    this.idService.streamRDF(streamRequest, cachedJobResult.getIdMappingResult());
            return super.streamRDF(result, streamRequest, contentType, request);
        } else {
            Stream<UniRefEntryPair> result =
                    this.idService.streamEntries(
                            streamRequest, cachedJobResult.getIdMappingResult());
            return super.stream(result, streamRequest, contentType, request);
        }
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
