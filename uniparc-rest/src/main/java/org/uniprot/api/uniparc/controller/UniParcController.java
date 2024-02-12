package org.uniprot.api.uniparc.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.uniparc.request.*;
import org.uniprot.api.uniparc.service.UniParcQueryService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Tag(name = TAG_UNIPARC, description = TAG_UNIPARC_DESCRIPTION)
@RestController
@Validated
@RequestMapping("/uniparc")
public class UniParcController extends BasicSearchController<UniParcEntry> {
    private static final String DATA_TYPE = "uniparc";

    private final UniParcQueryService queryService;
    private static final int PREVIEW_SIZE = 10;

    @Autowired
    public UniParcController(
            ApplicationEventPublisher eventPublisher,
            UniParcQueryService queryService,
            MessageConverterContextFactory<UniParcEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPARC,
                downloadGatekeeper);
        this.queryService = queryService;
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEARCH_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
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
    public ResponseEntity<MessageConverterContext<UniParcEntry>> search(
            @Valid @ModelAttribute UniParcSearchRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        setBasicRequestFormat(searchRequest, request);
        QueryResult<UniParcEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/{upi}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_UNIPARC_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpId(
            @Valid @ModelAttribute UniParcGetByUniParcIdRequest getByUniParcIdRequest,
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(request);
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String result =
                    queryService.getRdf(
                            getByUniParcIdRequest.getUpi(),
                            DATA_TYPE,
                            acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, contentType, request);
        }

        UniParcEntry entry = queryService.getByUniParcId(getByUniParcIdRequest);
        return super.getEntityResponse(entry, getByUniParcIdRequest.getFields(), request);
    }

    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = STREAM_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
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
    public DeferredResult<ResponseEntity<MessageConverterContext<UniParcEntry>>> stream(
            @Valid @ModelAttribute UniParcStreamRequest streamRequest,
            @Parameter(hidden = true)
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_XML_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        setBasicRequestFormat(streamRequest, request);
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            queryService.streamRdf(
                                    streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> queryService.stream(streamRequest), streamRequest, contentType, request);
        }
    }

    @GetMapping(
            value = "/accession/{accession}",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ACCESSION_UNIPARC_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByAccession(
            @Valid @ModelAttribute UniParcGetByAccessionRequest getByAccessionRequest,
            HttpServletRequest request) {

        UniParcEntry entry = queryService.getByUniProtAccession(getByAccessionRequest);

        return super.getEntityResponse(entry, getByAccessionRequest.getFields(), request);
    }

    @GetMapping(
            value = "/dbreference/{dbId}",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = DBID_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
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
    public ResponseEntity<MessageConverterContext<UniParcEntry>> searchByDBRefId(
            @Valid @ModelAttribute UniParcGetByDBRefIdRequest getByDbIdRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<UniParcEntry> results = queryService.searchByFieldId(getByDbIdRequest);

        return super.getSearchResponse(results, getByDbIdRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/proteome/{upId}",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = PROTEOME_UPID_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
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
    public ResponseEntity<MessageConverterContext<UniParcEntry>> searchByProteomeId(
            @Valid @ModelAttribute UniParcGetByProteomeIdRequest getByUpIdRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<UniParcEntry> results = queryService.searchByFieldId(getByUpIdRequest);

        return super.getSearchResponse(results, getByUpIdRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/bestguess",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, FASTA_MEDIA_TYPE_VALUE})
    @Operation(
            summary = BEST_GUESS_UNIPARC_OPERATION,
            description = BEST_GUESS_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> uniParcBestGuess(
            @Valid @ModelAttribute UniParcBestGuessRequest bestGuessRequest,
            HttpServletRequest request) {

        UniParcEntry bestGuess = queryService.getUniParcBestGuess(bestGuessRequest);

        return super.getEntityResponse(bestGuess, bestGuessRequest.getFields(), request);
    }

    @GetMapping(
            value = "/sequence",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEQUENCE_UNIPARC_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> uniParcSequenceFilter(
            @Valid @ModelAttribute UniParcSequenceRequest sequenceRequest,
            HttpServletRequest request) {

        UniParcEntry entry = queryService.getBySequence(sequenceRequest);

        return super.getEntityResponse(entry, sequenceRequest.getFields(), request);
    }

    @SuppressWarnings("squid:S3752")
    @RequestMapping(
            value = "/upis",
            method = {RequestMethod.GET},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = IDS_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            org.uniprot.core.xml
                                                                                    .jaxb.uniparc
                                                                                    .Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpisGet(
            @Valid @ModelAttribute UniParcIdsSearchRequest idsSearchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByUpis(idsSearchRequest, request, response);
    }

    @SuppressWarnings("squid:S3752")
    @RequestMapping(
            value = "/upis",
            method = {RequestMethod.POST},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            hidden = true,
            summary = IDS_UNIPARC_OPERATION,
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
                                                                            UniParcEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            org.uniprot.core.xml
                                                                                    .jaxb.uniparc
                                                                                    .Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpisPost(
            @Valid @NotNull(message = "{download.required}") @RequestBody(required = false)
                    UniParcIdsPostRequest idsSearchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByUpis(idsSearchRequest, request, response);
    }

    @Override
    protected String getEntityId(UniParcEntry entity) {
        return entity.getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniParcEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniParcSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }

    private ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpis(
            IdsSearchRequest idsSearchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<UniParcEntry> results = queryService.getByIds(idsSearchRequest);

        return super.getSearchResponse(
                results,
                idsSearchRequest.getFields(),
                idsSearchRequest.isDownload(),
                request,
                response);
    }
}
