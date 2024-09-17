package org.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.PROTEOME;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.proteome.request.ProteomeSearchRequest;
import org.uniprot.api.proteome.request.ProteomeStreamRequest;
import org.uniprot.api.proteome.service.ProteomeQueryService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.core.xml.jaxb.proteome.Proteome;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 24 Apr 2019
 */
@Tag(name = TAG_PROTEOME, description = TAG_PROTEOME_DESC)
@RestController
@Validated
@RequestMapping("/proteomes")
public class ProteomeController extends BasicSearchController<ProteomeEntry> {

    private final ProteomeQueryService queryService;

    @Autowired
    public ProteomeController(
            ApplicationEventPublisher eventPublisher,
            ProteomeQueryService queryService,
            @Qualifier("PROTEOME")
                    MessageConverterContextFactory<ProteomeEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                PROTEOME,
                downloadGatekeeper);
        this.queryService = queryService;
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEARCH_PROTEOME_OPERATION,
            description = SEARCH_OPERATION_DESC,
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
                                                                            ProteomeEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Proteome.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<ProteomeEntry>> search(
            @Valid @ModelAttribute ProteomeSearchRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<ProteomeEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/{upid}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_PROTEOME_OPERATION,
            description = ID_PROTEOME_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProteomeEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Proteome.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<ProteomeEntry>> getByUpId(
            @Parameter(description = UPID_PROTEOME_DESCRIPTION, example = UPID_PROTEOME_EXAMPLE)
                    @PathVariable("upid")
                    @Pattern(
                            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upid.value}")
                    String upid,
            @ValidReturnFields(uniProtDataType = UniProtDataType.PROTEOME)
                    @Parameter(
                            description = FIELDS_PROTEOME_DESCRIPTION,
                            example = FIELDS_PROTEOME_EXAMPLE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        ProteomeEntry entry = queryService.findByUniqueId(upid.toUpperCase());
        return super.getEntityResponse(entry, fields, request);
    }

    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = STREAM_PROTEOME_OPERATION,
            description = STREAM_OPERATION_DESC,
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
                                                                            ProteomeEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Proteome.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<ProteomeEntry>>> stream(
            @Valid @ModelAttribute ProteomeStreamRequest streamRequest,
            @Parameter(hidden = true)
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        return super.stream(
                () -> queryService.stream(streamRequest), streamRequest, contentType, request);
    }

    @Override
    protected String getEntityId(ProteomeEntry entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            ProteomeEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
