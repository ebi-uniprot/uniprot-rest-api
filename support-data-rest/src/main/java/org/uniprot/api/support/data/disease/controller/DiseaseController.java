package org.uniprot.api.support.data.disease.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.disease.request.DiseaseSearchRequest;
import org.uniprot.api.support.data.disease.request.DiseaseStreamRequest;
import org.uniprot.api.support.data.disease.service.DiseaseService;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.store.config.UniProtDataType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

@RestController
@RequestMapping("/diseases")
@Validated
@Tag(
        name = "Disease",
        description =
                "The human diseases in which proteins are involved are "
                        + "described in UniProtKB entries with a controlled vocabulary.")
public class DiseaseController extends BasicSearchController<DiseaseEntry> {
    private static final String DATA_TYPE = "diseases";
    @Autowired private DiseaseService diseaseService;
    public static final String ACCESSION_REGEX = "DI-(\\d{5})";

    protected DiseaseController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<DiseaseEntry> diseaseMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {

        super(
                eventPublisher,
                diseaseMessageConverterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.DISEASE,
                downloadGatekeeper);
    }

    @Operation(
            summary = "Get diseases by id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DiseaseEntry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{id}",
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
    public ResponseEntity<MessageConverterContext<DiseaseEntry>> getByAccession(
            @Parameter(description = "disease id to find")
                    @PathVariable("id")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.disease.invalid.id}")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.DISEASE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String result = this.diseaseService.getRdf(id, DATA_TYPE, acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, getAcceptHeader(request), request);
        }

        DiseaseEntry disease = this.diseaseService.findByUniqueId(id);
        return super.getEntityResponse(disease, fields, request);
    }

    @Operation(
            summary = "Search disease by given Lucene search query.",
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
                                                                            DiseaseEntry.class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<DiseaseEntry>> searchCursor(
            @Valid @ModelAttribute DiseaseSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<DiseaseEntry> results = this.diseaseService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = "Download disease by given Lucene search query.",
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
                                                                            DiseaseEntry.class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = OBO_MEDIA_TYPE_VALUE),
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
                OBO_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                    TURTLE_MEDIA_TYPE_VALUE,
                    N_TRIPLES_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<DiseaseEntry>>> stream(
            @Valid @ModelAttribute DiseaseStreamRequest streamRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () -> diseaseService.streamRdf(streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> diseaseService.stream(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(DiseaseEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            DiseaseEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
