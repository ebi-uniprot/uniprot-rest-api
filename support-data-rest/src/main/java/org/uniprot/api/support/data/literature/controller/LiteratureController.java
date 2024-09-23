package org.uniprot.api.support.data.literature.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.LITERATURE;

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
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.SearchResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.literature.request.LiteratureSearchRequest;
import org.uniprot.api.support.data.literature.request.LiteratureStreamRequest;
import org.uniprot.api.support.data.literature.service.LiteratureService;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Tag(name = TAG_LITERATURE, description = TAG_LITERATURE_DESC)
@RestController
@RequestMapping("/citations")
@Validated
public class LiteratureController extends BasicSearchController<LiteratureEntry> {
    private static final String DATA_TYPE = "citations";
    private final LiteratureService literatureService;
    private static final String LITERATURE_ID_REGEX = "^[0-9]+$|CI-\\w{1,13}$|IND[0-9]+$";

    public LiteratureController(
            ApplicationEventPublisher eventPublisher,
            LiteratureService literatureService,
            @Qualifier("literatureMessageConverterContextFactory")
                    MessageConverterContextFactory<LiteratureEntry>
                            literatureMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                literatureMessageConverterContextFactory,
                downloadTaskExecutor,
                LITERATURE,
                downloadGatekeeper);
        this.literatureService = literatureService;
    }

    @Operation(
            summary = ID_LIT_OPERATION,
            description = ID_LIT_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LiteratureEntry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{citationId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> getByLiteratureId(
            @Parameter(description = ID_LIT_DESCRIPTION, example = ID_LIT_EXAMPLE)
                    @PathVariable("citationId")
                    @Pattern(
                            regexp = LITERATURE_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.literature.invalid.id}")
                    String citationId,
            @Parameter(description = FIELDS_LIT_DESCRIPTION, example = FIELDS_LIT_EXAMPLE)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.LITERATURE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            if (citationId.startsWith("CI") || citationId.startsWith("IND")) {
                throw new ResourceNotFoundException("Unable to find citation " + citationId);
            }
            String result =
                    this.literatureService.getRdf(
                            citationId, DATA_TYPE, acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, getAcceptHeader(request), request);
        }
        LiteratureEntry literatureEntry = this.literatureService.findByUniqueId(citationId);
        return super.getEntityResponse(literatureEntry, fields, request);
    }

    @Operation(
            summary = SEARCH_LIT_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "LiteratureEntry",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SearchResult.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> search(
            @Valid @ModelAttribute LiteratureSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<LiteratureEntry> results = literatureService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = STREAM_LIT_OPERATION,
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
                                                                            LiteratureEntry
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<LiteratureEntry>>> stream(
            @Valid @ModelAttribute LiteratureStreamRequest streamRequest,
            @Parameter(hidden = true)
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            literatureService.streamRdf(
                                    streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> literatureService.stream(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(LiteratureEntry entity) {
        return entity.getCitation().getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            LiteratureEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
