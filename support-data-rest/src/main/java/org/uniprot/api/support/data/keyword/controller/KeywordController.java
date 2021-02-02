package org.uniprot.api.support.data.keyword.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.OBO_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.KEYWORD;

import java.util.Optional;
import java.util.stream.Stream;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Keyword",
        description =
                "UniProtKB Keywords constitute a controlled vocabulary with a hierarchical structure. Keywords summarise the content of a UniProtKB entry and facilitate the search for proteins of interest. An entry often contains several keywords. Keywords can be used to retrieve subsets of protein entries. Keywords are classified in 10 categories: Biological process, Cellular component, Coding sequence diversity, Developmental stage, DiseaseEntry, Domain, Ligand, Molecular function, Post-translational modification, Technical term.")
@RestController
@RequestMapping("/keyword")
@Validated
public class KeywordController extends BasicSearchController<KeywordEntry> {
    private final KeywordService keywordService;
    private static final String KEYWORD_ID_REGEX = "^KW-[0-9]{4}";

    public KeywordController(
            ApplicationEventPublisher eventPublisher,
            KeywordService keywordService,
            @Qualifier("keywordMessageConverterContextFactory")
                    MessageConverterContextFactory<KeywordEntry>
                            keywordMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, keywordMessageConverterContextFactory, downloadTaskExecutor, KEYWORD);
        this.keywordService = keywordService;
    }

    @GetMapping(
            value = "/{id}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
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
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
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

        KeywordEntry keywordEntry = this.keywordService.findByUniqueId(id);
        return super.getEntityResponse(keywordEntry, fields, request);
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
            summary = "Search Keywords by given SOLR search query.",
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
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
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
                RDF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Download Keywords by given SOLR search query.",
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
        if (contentType.equals(RDF_MEDIA_TYPE)) {
            Stream<String> result = keywordService.streamRDF(streamRequest);
            return super.streamRDF(result, streamRequest, contentType, request);
        } else {
            Stream<KeywordEntry> result = keywordService.stream(streamRequest);
            return super.stream(result, streamRequest, contentType, request);
        }
    }

    @Override
    protected String getEntityId(KeywordEntry entity) {
        return entity.getKeyword().getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(KeywordEntry entity) {
        return Optional.empty();
    }
}
