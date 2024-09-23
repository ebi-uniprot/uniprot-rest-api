package org.uniprot.api.support.data.keyword.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.KEYWORD;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.KEYWORD_ID_REGEX;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

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
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.SearchResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.common.keyword.request.KeywordSearchRequest;
import org.uniprot.api.support.data.common.keyword.request.KeywordStreamRequest;
import org.uniprot.api.support.data.common.keyword.service.KeywordService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = TAG_KEYWORDS, description = TAG_KEYWORDS_DESC)
@RestController
@RequestMapping("/keywords")
@Validated
public class KeywordController extends BasicSearchController<KeywordEntry> {
    private static final String DATA_TYPE = "keywords";
    private final KeywordService keywordService;

    public KeywordController(
            ApplicationEventPublisher eventPublisher,
            KeywordService keywordService,
            @Qualifier("keywordMessageConverterContextFactory")
                    MessageConverterContextFactory<KeywordEntry>
                            keywordMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                keywordMessageConverterContextFactory,
                downloadTaskExecutor,
                KEYWORD,
                downloadGatekeeper);
        this.keywordService = keywordService;
    }

    @GetMapping(
            value = "/{id}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_KEYWORDS_OPERATION,
            description = ID_KEYWORDS_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = KeywordEntry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<KeywordEntry>> getById(
            @Parameter(description = ID_KEYWORDS_DESCRIPTION, example = ID_KEYWORDS_EXAMPLE)
                    @PathVariable("id")
                    @Pattern(
                            regexp = KEYWORD_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.keyword.invalid.id}")
                    String id,
            @Parameter(description = FIELDS_KEYWORDS_DESCRIPTION, example = FIELDS_KEYWORDS_EXAMPLE)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.KEYWORD)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String result = this.keywordService.getRdf(id, DATA_TYPE, acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, getAcceptHeader(request), request);
        }
        KeywordEntry keywordEntry = this.keywordService.findByUniqueId(id);
        return super.getEntityResponse(keywordEntry, fields, request);
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEARCH_KEYWORDS_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "KeywordEntry",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SearchResult.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE),
                        })
            })
    public ResponseEntity<MessageConverterContext<KeywordEntry>> search(
            @Valid @ModelAttribute KeywordSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<KeywordEntry> results = keywordService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = STREAM_KEYWORDS_OPERATION,
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
                                                                            KeywordEntry.class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<KeywordEntry>>> stream(
            @Valid @ModelAttribute KeywordStreamRequest streamRequest, HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            keywordService.streamRdf(
                                    streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> keywordService.stream(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(KeywordEntry entity) {
        return entity.getKeyword().getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            KeywordEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
