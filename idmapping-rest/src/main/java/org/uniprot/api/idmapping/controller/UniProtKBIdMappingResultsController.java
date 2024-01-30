package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.Optional;
import java.util.function.Supplier;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.UniProtKBIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.core.xml.jaxb.uniprot.Entry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 17/02/2021
 */
@Tag(name = "results", description = "APIs to get result of the submitted job.")
@Slf4j
@RestController
@RequestMapping(
        value =
                IdMappingJobController.IDMAPPING_PATH
                        + "/"
                        + UniProtKBIdMappingResultsController.UNIPROTKB_ID_MAPPING_PATH)
public class UniProtKBIdMappingResultsController extends BasicSearchController<UniProtKBEntryPair> {
    private static final String DATA_TYPE = "uniprotkb";
    public static final String UNIPROTKB_ID_MAPPING_PATH = "uniprotkb";
    private final UniProtKBIdService idService;
    private final IdMappingJobCacheService cacheService;

    @Autowired
    public UniProtKBIdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            UniProtKBIdService idService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPROTKB,
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
                APPLICATION_XML_VALUE,
                FF_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary =
                    "Search result for a UniProtKB protein entry (or entries) mapped by a submitted job id.",
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
                                                                            UniProtKBEntryPair
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniProtKBEntryPair>> getMappedEntries(
            @Parameter(description = "Unique identifier for idmapping job") @PathVariable
                    String jobId,
            @Valid @ModelAttribute UniProtKBIdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob cachedJobResult = cacheService.getCompletedJobAsResource(jobId);

        QueryResult<UniProtKBEntryPair> result =
                this.idService.getMappedEntries(
                        searchRequest,
                        cachedJobResult.getIdMappingResult(),
                        cachedJobResult.getJobId());
        return super.getSearchResponse(
                result,
                searchRequest.getFields(),
                false,
                searchRequest.isSubSequence(),
                request,
                response);
    }

    @GetMapping(
            value = "/results/stream/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Download UniProtKB protein entry (or entries) mapped by a submitted job id.",
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
                                                                            UniProtKBEntryPair
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniProtKBEntryPair>>>
            streamMappedEntries(
                    @Parameter(description = "Unique identifier for idmapping job") @PathVariable
                            String jobId,
                    @Valid @ModelAttribute UniProtKBIdMappingStreamRequest streamRequest,
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
            Supplier<Stream<UniProtKBEntryPair>> result =
                    () ->
                            this.idService.streamEntries(
                                    streamRequest, idMappingResult, cachedJobResult.getJobId());
            return super.stream(result, streamRequest, contentType, request);
        }
    }

    @Override
    protected String getEntityId(UniProtKBEntryPair entity) {
        return entity.getTo().getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniProtKBEntryPair entity, HttpServletRequest request) {
        return Optional.empty();
    }

    @Override
    protected MessageConverterContext<UniProtKBEntryPair> createStreamContext(
            StreamRequest streamRequest, MediaType contentType, HttpServletRequest request) {
        UniProtKBIdMappingStreamRequest idMappingStreamRequest =
                (UniProtKBIdMappingStreamRequest) streamRequest;
        MessageConverterContext<UniProtKBEntryPair> context =
                super.createStreamContext(streamRequest, contentType, request);
        context.setSubsequence(idMappingStreamRequest.isSubSequence());
        return context;
    }
}
