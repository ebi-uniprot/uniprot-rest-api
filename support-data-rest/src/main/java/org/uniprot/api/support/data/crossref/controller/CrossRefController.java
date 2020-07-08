package org.uniprot.api.support.data.crossref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.CROSSREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.support.data.crossref.request.CrossRefSearchRequest;
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/xref")
@Validated
@Tag(
        name = "CrossReference",
        description =
                "The cross-references section of UniProtKB entries "
                        + "displays explicit and implicit links to databases such as nucleotide sequence databases, "
                        + "model organism databases and genomics and proteomics resources. A single entry can have "
                        + "cross-references to several dozen different databases and have several hundred individual links. "
                        + "The databases are categorized for easy user perusal and understanding of how the "
                        + "different databases relate to both UniProtKB and to each other")
public class CrossRefController extends BasicSearchController<CrossRefEntry> {
    @Autowired private CrossRefService crossRefService;
    private static final String ACCESSION_REGEX = "DB-(\\d{4})";

    public CrossRefController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory) {
        super(eventPublisher, crossrefMessageConverterContextFactory, null, CROSSREF);
    }

    @Operation(
            summary = "Get cross-references by database id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CrossRefEntry.class))
                        })
            })
    @GetMapping(
            value = "/{id}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> findByAccession(
            @Parameter(description = "cross-references database id to find")
                    @PathVariable("id")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.crossref.invalid.id}")
                    String id,
            @Parameter(description = "Comma separated list of fields to be returned in response")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.CROSSREF)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        CrossRefEntry crossRefEntry = this.crossRefService.findByUniqueId(id);

        return super.getEntityResponse(crossRefEntry, fields, request);
    }

    @Operation(
            summary = "Search cross-references by given SOLR search query.",
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
                                                                            CrossRefEntry.class)))
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

    @Override
    protected String getEntityId(CrossRefEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CrossRefEntry entity) {
        return Optional.empty();
    }
}
