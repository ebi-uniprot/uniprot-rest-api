package org.uniprot.api.support.data.subcellular.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.OBO_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.SUBCELLULAR_LOCATION;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.subcellular.request.SubcellularLocationSearchRequest;
import org.uniprot.api.support.data.subcellular.request.SubcellularLocationStreamRequest;
import org.uniprot.api.support.data.subcellular.service.SubcellularLocationService;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
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
 * @since 2019-07-19
 */
@Tag(
        name = "Subcellular location",
        description =
                "The subcellular locations in which a protein is found are described in UniProtKB entries with a controlled vocabulary, which includes also membrane topology and orientation terms.")
@RestController
@RequestMapping("/locations")
@Validated
public class SubcellularLocationController extends BasicSearchController<SubcellularLocationEntry> {

    private final SubcellularLocationService subcellularLocationService;
    private static final String SUBCELLULAR_LOCATION_ID_REGEX = "^SL-[0-9]{4}";

    public SubcellularLocationController(
            ApplicationEventPublisher eventPublisher,
            SubcellularLocationService subcellularLocationService,
            @Qualifier("subcellularLocationMessageConverterContextFactory")
                    MessageConverterContextFactory<SubcellularLocationEntry>
                            subcellularLocationMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                subcellularLocationMessageConverterContextFactory,
                downloadTaskExecutor,
                SUBCELLULAR_LOCATION,
                downloadGatekeeper);
        this.subcellularLocationService = subcellularLocationService;
    }

    @Operation(
            summary = "Get subcellular locations by id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema =
                                            @Schema(
                                                    implementation =
                                                            SubcellularLocationEntry.class)),
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
                RDF_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<SubcellularLocationEntry>> getById(
            @Parameter(description = "Subcellular location id to find")
                    @PathVariable("id")
                    @Pattern(
                            regexp = SUBCELLULAR_LOCATION_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.subcellularLocation.invalid.id}")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        if (isRDFAccept(request)) {
            String result = this.subcellularLocationService.getRDFXml(id);
            return super.getEntityResponseRDF(result, getAcceptHeader(request), request);
        }

        SubcellularLocationEntry subcellularLocationEntry =
                this.subcellularLocationService.findByUniqueId(id);
        return super.getEntityResponse(subcellularLocationEntry, fields, request);
    }

    @Operation(
            summary = "Search subcellular locations by given Lucene search query.",
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
                                                                            SubcellularLocationEntry
                                                                                    .class))),
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
    public ResponseEntity<MessageConverterContext<SubcellularLocationEntry>> search(
            @Valid @ModelAttribute SubcellularLocationSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<SubcellularLocationEntry> results =
                subcellularLocationService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Operation(
            summary = "Download subcellular locations by given Lucene search query.",
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
                                                                            SubcellularLocationEntry
                                                                                    .class))),
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
                RDF_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<SubcellularLocationEntry>>> stream(
            @Valid @ModelAttribute SubcellularLocationStreamRequest streamRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        if (contentType.equals(RDF_MEDIA_TYPE)) {
            return super.streamRDF(
                    () -> subcellularLocationService.streamRDF(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> subcellularLocationService.stream(streamRequest),
                    streamRequest,
                    contentType,
                    request);
        }
    }

    @Override
    protected String getEntityId(SubcellularLocationEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            SubcellularLocationEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
