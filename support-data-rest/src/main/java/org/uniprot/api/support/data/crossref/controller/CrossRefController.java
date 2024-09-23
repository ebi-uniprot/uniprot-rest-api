package org.uniprot.api.support.data.crossref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.CROSSREF;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.CROSS_REF_REGEX;

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
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.SearchResult;
import org.uniprot.api.rest.openapi.StreamResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.crossref.request.CrossRefSearchRequest;
import org.uniprot.api.support.data.crossref.request.CrossRefStreamRequest;
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/database")
@Validated
@Tag(name = TAG_CROSSREF, description = TAG_CROSSREF_DESC)
public class CrossRefController extends BasicSearchController<CrossRefEntry> {
    private static final String DATA_TYPE = "databases";
    @Autowired private CrossRefService crossRefService;

    public CrossRefController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                crossrefMessageConverterContextFactory,
                downloadTaskExecutor,
                CROSSREF,
                downloadGatekeeper);
    }

    @Operation(
            summary = ID_CROSSREF_OPERATION,
            description = ID_CROSSREF_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CrossRefEntry.class)),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{id}",
            produces = {
                APPLICATION_JSON_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> findByAccession(
            @Parameter(description = ID_CROSSREF_DESCRIPTION, example = ID_CROSSREF_EXAMPLE)
                    @PathVariable("id")
                    @Pattern(
                            regexp = CROSS_REF_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.crossref.invalid.id}")
                    String id,
            @Parameter(description = FIELDS_CROSSREF_DESCRIPTION, example = FIELDS_CROSSREF_EXAMPLE)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.CROSSREF)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String result =
                    this.crossRefService.getRdf(id, DATA_TYPE, acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, getAcceptHeader(request), request);
        }

        CrossRefEntry crossRefEntry = this.crossRefService.findByUniqueId(id);

        return super.getEntityResponse(crossRefEntry, fields, request);
    }

    @Operation(
            summary = SEARCH_CROSSREF_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "CrossRefEntry",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SearchResult.class))
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> search(
            @Valid @ModelAttribute CrossRefSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<CrossRefEntry> results = this.crossRefService.search(searchRequest);

        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = STREAM_CROSSREF_OPERATION,
            description = STREAM_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StreamResult.class)),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/stream",
            produces = {
                APPLICATION_JSON_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<CrossRefEntry>>> stream(
            @Valid @ModelAttribute CrossRefStreamRequest streamRequest,
            @Parameter(hidden = true)
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            crossRefService.streamRdf(
                                    streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> crossRefService.stream(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(CrossRefEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            CrossRefEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
