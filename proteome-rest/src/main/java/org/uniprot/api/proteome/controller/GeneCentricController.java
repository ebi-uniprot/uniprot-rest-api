package org.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.GENECENTRIC;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.proteome.request.GeneCentricSearchRequest;
import org.uniprot.api.proteome.request.GeneCentricStreamRequest;
import org.uniprot.api.proteome.request.GeneCentricUPIdRequest;
import org.uniprot.api.proteome.service.GeneCentricService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
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
 * @author jluo
 * @date: 30 Apr 2019
 */
@RestController
@Validated
@RequestMapping("/genecentric")
public class GeneCentricController extends BasicSearchController<GeneCentricEntry> {

    private final GeneCentricService service;
    private final String upIdFieldName;

    @Autowired
    public GeneCentricController(
            ApplicationEventPublisher eventPublisher,
            GeneCentricService service,
            @Qualifier("GENECENTRIC")
                    MessageConverterContextFactory<GeneCentricEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            SearchFieldConfig geneCentricSearchFieldConfig) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, GENECENTRIC);
        this.service = service;
        this.upIdFieldName =
                geneCentricSearchFieldConfig.getSearchFieldItemByName("upid").getFieldName();
    }

    @Tag(name = "genecentric", description = "gene centric service")
    @Operation(
            summary = "Search for gene centric data set.",
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
                                                                            GeneCentricEntry
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            GeneCentricEntry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<GeneCentricEntry>> searchCursor(
            @Valid @ModelAttribute GeneCentricSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<GeneCentricEntry> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "genecentric")
    @GetMapping(
            value = "/upid/{upid}",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Fetch all proteins of Proteome id.",
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
                                                                            GeneCentricEntry
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            GeneCentricEntry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<GeneCentricEntry>> getByUpId(
            @Valid @ModelAttribute GeneCentricUPIdRequest upIdRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        GeneCentricSearchRequest searchRequest = convertUPIdRequestToSearchRequest(upIdRequest);
        QueryResult<GeneCentricEntry> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "genecentric")
    @Operation(
            summary = "Download Gene Centric data retrieved by search.",
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
                                                                            GeneCentricEntry
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            GeneCentricEntry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/stream",
            produces = {
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<GeneCentricEntry>>> stream(
            @Valid @ModelAttribute GeneCentricStreamRequest streamRequest,
            HttpServletRequest request) {
        return super.stream(
                () -> service.stream(streamRequest),
                streamRequest,
                getAcceptHeader(request),
                request);
    }

    @Tag(name = "genecentric")
    @Operation(
            summary = "Retrieve an gene centric entry by uniprot accession.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GeneCentricEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = GeneCentricEntry.class)),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{accession}",
            produces = {
                APPLICATION_JSON_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<GeneCentricEntry>> getByAccession(
            @Parameter(description = "UnirotKB accession")
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "genecentric-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        GeneCentricEntry entry = service.findByUniqueId(accession.toUpperCase());
        return super.getEntityResponse(entry, fields, request);
    }

    @Override
    protected String getEntityId(GeneCentricEntry entity) {
        return entity.getCanonicalProtein().getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            GeneCentricEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }

    private GeneCentricSearchRequest convertUPIdRequestToSearchRequest(
            @ModelAttribute @Valid GeneCentricUPIdRequest upIdRequest) {
        GeneCentricSearchRequest searchRequest = new GeneCentricSearchRequest();
        searchRequest.setQuery(upIdFieldName + ":" + upIdRequest.getUpid());
        searchRequest.setFields(upIdRequest.getFields());
        searchRequest.setCursor(upIdRequest.getCursor());
        searchRequest.setSize(upIdRequest.getSize());
        return searchRequest;
    }
}
