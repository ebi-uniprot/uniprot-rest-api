package org.uniprot.api.support.data.literature.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.LITERATURE;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.literature.request.LiteratureRequest;
import org.uniprot.api.support.data.literature.service.LiteratureService;
import org.uniprot.core.citation.Literature;
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
@Tag(
        name = "Literature",
        description =
                "Search publications that are cited in, or were computationally mapped to, UniProtKB")
@RestController
@RequestMapping("/literature")
@Validated
public class LiteratureController extends BasicSearchController<LiteratureEntry> {

    private final LiteratureService literatureService;
    private static final String LITERATURE_ID_REGEX = "^[0-9]+$";

    public LiteratureController(
            ApplicationEventPublisher eventPublisher,
            LiteratureService literatureService,
            @Qualifier("literatureMessageConverterContextFactory")
                    MessageConverterContextFactory<LiteratureEntry>
                            literatureMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(
                eventPublisher,
                literatureMessageConverterContextFactory,
                downloadTaskExecutor,
                LITERATURE);
        this.literatureService = literatureService;
    }

    @Operation(
            summary = "Get literature by PubMed id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LiteratureEntry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{pubMedId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> getByLiteratureId(
            @Parameter(description = "PubMed id to find")
                    @PathVariable("pubMedId")
                    @Pattern(
                            regexp = LITERATURE_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.literature.invalid.id}")
                    String pubMedId,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.LITERATURE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        LiteratureEntry literatureEntry = this.literatureService.findByUniqueId(pubMedId);
        return super.getEntityResponse(literatureEntry, fields, request);
    }

    @Operation(
            summary = "Search literature by given SOLR search query.",
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
            @Valid @ModelAttribute LiteratureRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<LiteratureEntry> results = literatureService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = "Download literature by given SOLR search query.",
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
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/download",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<LiteratureEntry>>> download(
            @Valid @ModelAttribute LiteratureRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        Stream<LiteratureEntry> result = literatureService.download(searchRequest);
        return super.stream(result, searchRequest.getFields(), contentType, request);
    }

    @Override
    protected String getEntityId(LiteratureEntry entity) {
        Literature literature = (Literature) entity.getCitation();
        return String.valueOf(literature.getPubmedId());
    }

    @Override
    protected Optional<String> getEntityRedirectId(LiteratureEntry entity) {
        return Optional.empty();
    }
}
