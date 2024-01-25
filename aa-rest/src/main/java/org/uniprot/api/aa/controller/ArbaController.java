package org.uniprot.api.aa.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.aa.request.ArbaSearchRequest;
import org.uniprot.api.aa.request.ArbaStreamRequest;
import org.uniprot.api.aa.service.ArbaService;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Tag(
        name = "arba",
        description =
                "The Association-Rule-Based Annotator(ARBA) resource for automatic annotation in the UniProt Knowledgebase ")
@RestController
@Validated
@RequestMapping("/arba")
public class ArbaController extends BasicSearchController<UniRuleEntry> {
    private static final int PREVIEW_SIZE = 10;
    private final ArbaService arbaService;
    public static final String ARBA_ID_REGEX = "ARBA(\\d{8})";

    @Autowired
    public ArbaController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniRuleEntry> arbaMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            ArbaService arbaService,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                arbaMessageConverterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.ARBA,
                downloadGatekeeper);
        this.arbaService = arbaService;
    }

    @GetMapping(
            value = "/{arbaId}",
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Get ArbaRule entry by a single arbaId.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniRuleEntry.class)),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRuleEntry>> getByArbaId(
            @Parameter(description = "Get Arba entry by an arbaId")
            @PathVariable("arbaId")
                    @Pattern(
                            regexp = ARBA_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.arba.invalid.id}")
                    String arbaId,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.ARBA)
                    String fields,
            HttpServletRequest request) {
        UniRuleEntry entryResult = this.arbaService.findByUniqueId(arbaId);
        return super.getEntityResponse(entryResult, fields, request);
    }

    @GetMapping(
            value = "/search",
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Retrieve ArbaRule entries by a search query.",
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
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRuleEntry>> search(
            @Valid @ModelAttribute ArbaSearchRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniRuleEntry> results = arbaService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/stream",
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Download ArbaRule entries retrieved by a search query.",
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
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRuleEntry>>> stream(
            @Valid @ModelAttribute ArbaStreamRequest streamRequest, HttpServletRequest request) {

        return super.stream(
                () -> arbaService.stream(streamRequest),
                streamRequest,
                getAcceptHeader(request),
                request);
    }

    @Override
    protected String getEntityId(UniRuleEntry entity) {
        return entity.getUniRuleId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniRuleEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }

    private void setPreviewInfo(ArbaSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
