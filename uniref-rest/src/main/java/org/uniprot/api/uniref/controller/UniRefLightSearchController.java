package org.uniprot.api.uniref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import org.uniprot.api.uniref.request.UniRefSearchRequest;
import org.uniprot.api.uniref.request.UniRefStreamRequest;
import org.uniprot.api.uniref.service.UniRefLightSearchService;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;

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
public class UniRefLightSearchController extends BasicSearchController<UniRefEntryLight> {

    private static final int PREVIEW_SIZE = 10;
    private final UniRefLightSearchService queryService;
    private final MessageConverterContextFactory<UniRefEntryLight> converterContextFactory;

    @Autowired
    public UniRefLightSearchController(
            ApplicationEventPublisher eventPublisher,
            UniRefLightSearchService queryService,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIREF);
        this.queryService = queryService;
        this.converterContextFactory = converterContextFactory;
    }

    @Tag(name = "uniref")
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
        QueryResult<UniRefEntryLight> results = queryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "uniref")
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE
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

        MessageConverterContext<UniRefEntryLight> context =
                converterContextFactory.get(UNIREF, contentType);
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

    @Override
    protected String getEntityId(UniRefEntryLight entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniRefEntryLight entity) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniRefSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
