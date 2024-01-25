package org.uniprot.api.uniref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniref.common.request.UniRefSearchRequest;
import org.uniprot.api.uniref.common.request.UniRefStreamRequest;
import org.uniprot.api.uniref.common.service.UniRefEntryLightService;
import org.uniprot.api.uniref.request.UniRefIdsPostRequest;
import org.uniprot.api.uniref.request.UniRefIdsSearchRequest;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@RestController
@Validated
@RequestMapping("/uniref")
public class UniRefEntryLightController extends BasicSearchController<UniRefEntryLight> {
    private static final String DATA_TYPE = "uniref";
    private static final int PREVIEW_SIZE = 10;
    private final UniRefEntryLightService service;

    @Autowired
    public UniRefEntryLightController(
            ApplicationEventPublisher eventPublisher,
            UniRefEntryLightService queryService,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIREF,
                downloadGatekeeper);
        this.service = queryService;
    }

    @Tag(
            name = "uniref",
            description =
                    "The UniProt Reference Clusters (UniRef) provide clustered sets of sequences from the UniProt Knowledgebase (including isoforms) and selected UniParc records. This hides redundant sequences and obtains complete coverage of the sequence space at three resolutions: UniRef100, UniRef90 and UniRef50.")
    @GetMapping(
            value = "/{id}/light",
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
            summary = "Retrieve a light object of UniRef cluster by id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniRefEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> getById(
            @Parameter(description = "Unique identifier for the UniRef cluster")
                    @PathVariable("id")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.id.value}")
                    String id,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "uniref-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
                    @Parameter(
                            description =
                                    "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String rdf = service.getRdf(id, DATA_TYPE, acceptedRdfContentType.get());
            return super.getEntityResponseRdf(rdf, getAcceptHeader(request), request);
        } else {
            UniRefEntryLight entryResult = service.findByUniqueId(id, fields);
            return super.getEntityResponse(entryResult, fields, request);
        }
    }

    @Tag(name = "uniref")
    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Search for a UniRef cluster (or clusters) by a SOLR query.",
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
                                                                            UniRefEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            org.uniprot.core.xml
                                                                                    .jaxb.uniref
                                                                                    .Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> search(
            @Valid @ModelAttribute UniRefSearchRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        setBasicRequestFormat(searchRequest, request);
        QueryResult<UniRefEntryLight> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "uniref")
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Stream an UniRef cluster (or clusters) retrieved by a SOLR query.",
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
                                                                            UniRefEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            org.uniprot.core.xml
                                                                                    .jaxb.uniref
                                                                                    .Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefEntryLight>>> stream(
            @Valid @ModelAttribute UniRefStreamRequest streamRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_XML_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        setBasicRequestFormat(streamRequest, request);
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () -> service.streamRdf(streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> service.stream(streamRequest), streamRequest, contentType, request);
        }
    }

    @SuppressWarnings("squid:S3752")
    @Tag(name = "uniref")
    @RequestMapping(
            value = "/ids",
            method = {RequestMethod.GET},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Get UniRef entries by a list of ids.",
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
                                                                            UniRefEntryLight
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> getByIdsGet(
            @Valid @ModelAttribute UniRefIdsSearchRequest idsRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByIds(idsRequest, request, response);
    }

    @SuppressWarnings("squid:S3752")
    @Tag(name = "uniref")
    @RequestMapping(
            value = "/ids",
            method = {RequestMethod.POST},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Get UniRef entries by a list of ids.",
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
                                                                            UniRefEntryLight
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> getByIdsPost(
            @Valid @NotNull(message = "{download.required}") @RequestBody(required = false)
                    UniRefIdsPostRequest idsRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByIds(idsRequest, request, response);
    }

    @Override
    protected String getEntityId(UniRefEntryLight entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniRefEntryLight entity, HttpServletRequest request) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniRefSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }

    private ResponseEntity<MessageConverterContext<UniRefEntryLight>> getByIds(
            IdsSearchRequest idsRequest, HttpServletRequest request, HttpServletResponse response) {
        QueryResult<UniRefEntryLight> results = service.getByIds(idsRequest);
        return super.getSearchResponse(
                results, idsRequest.getFields(), idsRequest.isDownload(), request, response);
    }
}
