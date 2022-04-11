package org.uniprot.api.aa.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIRULE;

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
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Hidden
@Tag(
        name = "unirule",
        description =
                "The unified rule(UniRule) resource for automatic annotation in the UniProt Knowledgebase ")
@RestController
@Validated
@RequestMapping("/unirule")
public class UniRuleController extends BasicSearchController<UniRuleEntry> {

    private static final int PREVIEW_SIZE = 10;
    private final UniRuleService uniRuleService;
    public static final String UNIRULE_ALL_ID_REGEX =
            "UR[0-9]{9}|MF_[0-9]{5}|PIRSR[0-9]+(\\-[0-9]+)?|PIRNR[0-9]+|RU[0-9]{6}|PRU[0-9]{5}";
    private static final String UNIRULE_ID_REGEX = "UR[0-9]{9}";
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

    @Hidden
    @GetMapping(
            value = "/{uniruleid}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Get a UniRule by id.",
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
            @PathVariable("uniruleid")
                    @Pattern(
                            regexp = UNIRULE_ALL_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.unirule.invalid.id}")
                    String uniRuleId,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE)
                    String fields,
            HttpServletRequest request) {
        UniRuleEntry entryResult = this.uniRuleService.findByUniqueId(uniRuleId);
        return super.getEntityResponse(entryResult, fields, request);
    }

    @Hidden
    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Search for a UniRule entry (or entries) by a Lucene query.",
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
                                                                            UniRuleEntry.class))),
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

    @Hidden
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Stream a UniRule entry (or entries) by a Lucene query.",
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
                                                                            UniRuleEntry.class))),
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
