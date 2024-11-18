package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.idmapping.common.service.IdMappingJobService.IDMAPPING_PATH;
import static org.uniprot.api.idmapping.common.service.IdMappingJobService.UNIPARC_ID_MAPPING_PATH;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.uniparc.UniParcIdMappingSearchRequest;
import org.uniprot.api.idmapping.common.request.uniparc.UniParcIdMappingStreamRequest;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.impl.UniParcLightIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.IdMappingSearchResult;
import org.uniprot.api.rest.openapi.StreamResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.xml.jaxb.uniparc.Entry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Tag(name = TAG_IDMAPPING_RESULT, description = TAG_IDMAPPING_RESULT_DESC)
@RestController
@RequestMapping(value = IDMAPPING_PATH + "/" + UNIPARC_ID_MAPPING_PATH)
public class UniParcIdMappingResultsController
        extends BasicSearchController<UniParcEntryLightPair> {
    private static final String DATA_TYPE = "uniparc";
    private final UniParcLightIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniParcIdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            UniParcLightIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniParcEntryLightPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPARC,
                downloadGatekeeper);
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
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = IDMAPPING_UNIPARC_RESULT_SEARCH_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "UniParcEntryPair",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = IdMappingSearchResult.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntryLightPair>> getMappedEntries(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Valid @ModelAttribute UniParcIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniParcEntryLightPair> result =
                this.idService.getMappedEntries(
                        searchRequest,
                        cachedJobResult.getIdMappingResult(),
                        cachedJobResult.getJobId());

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = IDMAPPING_UNIPARC_RESULT_STREAM_OPERATION,
            description = STREAM_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StreamResult.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniParcEntryLightPair>>>
            streamMappedEntries(
                    @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable
                            String jobId,
                    @Valid @ModelAttribute UniParcIdMappingStreamRequest streamRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {

        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);
        MediaType contentType = getAcceptHeader(request);

        IdMappingResult idMappingResult = cachedJobResult.getIdMappingResult();
        this.idService.validateMappedIdsEnrichmentLimit(idMappingResult.getMappedIds());

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            Supplier<Stream<String>> result =
                    () ->
                            this.idService.streamRdf(
                                    streamRequest,
                                    idMappingResult,
                                    cachedJobResult.getJobId(),
                                    DATA_TYPE,
                                    acceptedRdfContentType.get());
            return super.streamRdf(result, streamRequest, contentType, request);
        } else {
            Supplier<Stream<UniParcEntryLightPair>> result =
                    () ->
                            this.idService.streamEntries(
                                    streamRequest, idMappingResult, cachedJobResult.getJobId());
            return super.stream(result, streamRequest, contentType, request);
        }
    }

    @Override
    protected String getEntityId(UniParcEntryLightPair entity) {
        return entity.getTo().getUniParcId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniParcEntryLightPair entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
