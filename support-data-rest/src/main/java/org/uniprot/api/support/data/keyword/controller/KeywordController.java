package org.uniprot.api.support.data.keyword.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.keyword.request.KeywordSearchRequest;
import org.uniprot.api.support.data.keyword.request.KeywordStreamRequest;
import org.uniprot.api.support.data.keyword.service.KeywordService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.UniProtDataType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.KEYWORD;

@Tag(
        name = "Keyword",
        description =
                "UniProtKB Keywords constitute a controlled vocabulary with a hierarchical structure. Keywords summarise the content of a UniProtKB entry and facilitate the search for proteins of interest. An entry often contains several keywords. Keywords can be used to retrieve subsets of protein entries. Keywords are classified in 10 categories: Biological process, Cellular component, Coding sequence diversity, Developmental stage, DiseaseEntry, Domain, Ligand, Molecular function, Post-translational modification, Technical term.")
@RestController
@RequestMapping("/keywords")
@Validated
public class KeywordController extends BasicSearchController<KeywordEntry> {
    private static final String TYPE = "keywords";
    private final KeywordService keywordService;
    private static final String KEYWORD_ID_REGEX = "^KW-[0-9]{4}";

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
                TTL_MEDIA_TYPE_VALUE,
                NT_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Get Keyword by keywordId.",
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
            @Parameter(description = "Keyword id to find")
                    @PathVariable("id")
                    @Pattern(
                            regexp = KEYWORD_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.keyword.invalid.id}")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.KEYWORD)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        Optional<String> acceptedRDFContentType = getAcceptedRDFContentType(request);
        if (acceptedRDFContentType.isPresent()) {
            String result = this.keywordService.getRDFXml(id, TYPE, acceptedRDFContentType.get());
            return super.getEntityResponseRDF(result, getAcceptHeader(request), request);
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
            summary = "Search Keywords by given Lucene search query.",
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
                TTL_MEDIA_TYPE_VALUE,
                NT_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Download Keywords by given Lucene search query.",
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
        Optional<String> acceptedRDFContentType = getAcceptedRDFContentType(request);
        if (acceptedRDFContentType.isPresent()) {
            return super.streamRDF(
                    () -> keywordService.streamRDF(streamRequest, TYPE, acceptedRDFContentType.get()),
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
