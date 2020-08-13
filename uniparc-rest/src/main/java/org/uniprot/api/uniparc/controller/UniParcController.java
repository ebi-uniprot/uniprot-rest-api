package org.uniprot.api.uniparc.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniparc.request.UniParcGetByAccessionRequest;
import org.uniprot.api.uniparc.request.UniParcGetByDbIdRequest;
import org.uniprot.api.uniparc.request.UniParcGetByUpIdRequest;
import org.uniprot.api.uniparc.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.request.UniParcStreamRequest;
import org.uniprot.api.uniparc.service.UniParcQueryService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
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
 * @author jluo
 * @date: 21 Jun 2019
 */
@RestController
@Validated
@RequestMapping("/uniparc")
public class UniParcController extends BasicSearchController<UniParcEntry> {

    private final UniParcQueryService queryService;
    private static final int PREVIEW_SIZE = 10;
    private final MessageConverterContextFactory<UniParcEntry> converterContextFactory;

    @Autowired
    public UniParcController(
            ApplicationEventPublisher eventPublisher,
            UniParcQueryService queryService,
            MessageConverterContextFactory<UniParcEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
        this.queryService = queryService;
        this.converterContextFactory = converterContextFactory;
    }

    @Tag(
            name = "uniparc",
            description =
                    "UniParc is a comprehensive and non-redundant database that contains most of the publicly available protein sequences in the world. Proteins may exist in different source databases and in multiple copies in the same database. UniParc avoids such redundancy by storing each unique sequence only once and giving it a stable and unique identifier (UPI).")
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
            summary = "Search for a UniParc sequence entry (or entries) by a SOLR query.",
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
        QueryResult<UniParcEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "uniparc")
    @GetMapping(
            value = "/{upi}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Retrieve an UniParc entry by upi.",
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
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpId(
            @Parameter(description = "Unique identifier for the UniParc entry")
                    @PathVariable("upi")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upi.value}")
                    String upi,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "uniparc-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
                    @Parameter(
                            description =
                                    "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        UniParcEntry entry = queryService.findByUniqueId(upi.toUpperCase());
        return super.getEntityResponse(entry, fields, request);
    }

    @Tag(name = "uniparc")
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Stream a UniParc sequence entry (or entries) by a SOLR query.",
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
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_XML_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        MessageConverterContext<UniParcEntry> context =
                converterContextFactory.get(UNIPARC, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setFields(streamRequest.getFields());
        context.setDownloadContentDispositionHeader(streamRequest.isDownload());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(queryService.streamIds(streamRequest));
        } else {
            context.setEntities(queryService.stream(streamRequest));
        }

        return super.getDeferredResultResponseEntity(request, context);
    }

    @GetMapping(
            value = "/accession/{accession}",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, FASTA_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniParcEntry>> findByAccession(
            @Valid @ModelAttribute UniParcGetByAccessionRequest getByAccessionRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<UniParcEntry> results = queryService.findByAccession(getByAccessionRequest);

        return super.getSearchResponse(
                results, getByAccessionRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/dbreference/{dbId}",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, FASTA_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniParcEntry>> findByDbId(
            @Valid @ModelAttribute UniParcGetByDbIdRequest getByDbIdRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<UniParcEntry> results = queryService.findByDbId(getByDbIdRequest);

        return super.getSearchResponse(results, getByDbIdRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/proteome/{upId}",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, FASTA_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniParcEntry>> findByUpId(
            @Valid @ModelAttribute UniParcGetByUpIdRequest getByUpIdRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<UniParcEntry> results = queryService.findByUpId(getByUpIdRequest);

        return super.getSearchResponse(results, getByUpIdRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(UniParcEntry entity) {
        return entity.getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniParcEntry entity) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniParcSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
