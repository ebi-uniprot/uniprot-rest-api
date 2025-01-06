package org.uniprot.api.aa.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIRULE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.*;

import java.util.Optional;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.aa.request.UniRuleSearchRequest;
import org.uniprot.api.aa.request.UniRuleStreamRequest;
import org.uniprot.api.aa.service.UniRuleService;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.SearchResult;
import org.uniprot.api.rest.openapi.StreamResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Tag(name = TAG_UNIRULE, description = TAG_UNIRULE_DESC)
@RestController
@Validated
@RequestMapping("/unirule")
public class UniRuleController extends BasicSearchController<UniRuleEntry> {

    private static final int PREVIEW_SIZE = 10;
    private final UniRuleService uniRuleService;
    private static final java.util.regex.Pattern UNIRULE_ID_PATTERN =
            java.util.regex.Pattern.compile(UNIRULE_ID_REGEX);

    @Autowired
    public UniRuleController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniRuleEntry> uniRuleMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            UniRuleService uniRuleService,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                uniRuleMessageConverterContextFactory,
                downloadTaskExecutor,
                UNIRULE,
                downloadGatekeeper);
        this.uniRuleService = uniRuleService;
    }

    @GetMapping(
            value = "/{uniruleid}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_UNIRULE_OPERATION,
            description = ID_UNIRULE_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniRuleEntry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRuleEntry>> getByUniRuleId(
            @Parameter(description = ID_UNIRULE_DESCRIPTION, example = ID_UNIRULE_EXAMPLE)
                    @PathVariable("uniruleid")
                    @Pattern(
                            regexp = UNIRULE_ALL_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.unirule.invalid.id}")
                    String uniRuleId,
            @Parameter(description = FIELDS_UNIRULE_DESCRIPTION, example = FIELDS_UNIRULE_EXAMPLE)
                    @RequestParam(value = "fields", required = false)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE)
                    String fields,
            HttpServletRequest request) {
        UniRuleEntry entryResult = this.uniRuleService.findByUniqueId(uniRuleId);
        return super.getEntityResponse(entryResult, fields, request);
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEARCH_UNIRULE_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "UniRuleEntry",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SearchResult.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRuleEntry>> search(
            @Valid @ModelAttribute UniRuleSearchRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniRuleEntry> results = uniRuleService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = STREAM_UNIRULE_OPERATION,
            description = STREAM_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StreamResult.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRuleEntry>>> stream(
            @Valid @ModelAttribute UniRuleStreamRequest streamRequest, HttpServletRequest request) {

        return super.stream(
                () -> uniRuleService.stream(streamRequest),
                streamRequest,
                getAcceptHeader(request),
                request);
    }

    @Override
    protected String getEntityId(UniRuleEntry entity) {
        return entity.getUniRuleId().getValue();
    }

    @Override
    protected String getFromId(UniRuleEntry entity, HttpServletRequest request) {
        return extractRuleId(request);
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniRuleEntry entity, HttpServletRequest request) {
        String ruleId = extractRuleId(request);
        Matcher matcher = UNIRULE_ID_PATTERN.matcher(ruleId);
        if (!matcher.find()) {
            return Optional.of(entity.getUniRuleId().getValue());
        }
        return Optional.empty();
    }

    private void setPreviewInfo(UniRuleSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }

    private String extractRuleId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ruleId = uri.substring(uri.lastIndexOf("/") + 1);
        return ruleId;
    }
}
