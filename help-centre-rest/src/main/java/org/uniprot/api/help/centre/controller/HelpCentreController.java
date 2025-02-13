package org.uniprot.api.help.centre.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.ID_HELP_OPERATION;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.help.centre.request.HelpCentreSearchRequest;
import org.uniprot.api.help.centre.service.HelpCentreService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.SearchResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Tag(name = TAG_HELP, description = TAG_HELP_DESC)
@RestController
@Validated
@RequestMapping("/help")
public class HelpCentreController extends BasicSearchController<HelpCentreEntry> {

    private static final String HELP_CENTRE_ID_REGEX = "(?!^[0-9]+$)^.+$";
    private final HelpCentreService service;
    public static final String HELP_STR = "help";

    protected HelpCentreController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<HelpCentreEntry> converterContextFactory,
            HelpCentreService service) {
        super(
                eventPublisher,
                converterContextFactory,
                null,
                MessageConverterContextFactory.Resource.HELP);
        this.service = service;
    }

    @Operation(
            summary = ID_HELP_OPERATION,
            description = ID_HELP_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = HelpCentreEntry.class)),
                            @Content(mediaType = MARKDOWN_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{id}",
            produces = {APPLICATION_JSON_VALUE, MARKDOWN_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<HelpCentreEntry>> getByHelpCentrePageId(
            @Parameter(description = ID_HELP_DESCRIPTION)
                    @Pattern(
                            regexp = HELP_CENTRE_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.helpcentre.invalid.id}")
                    @PathVariable("id")
                    String id,
            @Parameter(description = FIELDS_HELP_DESCRIPTION)
                    @ValidReturnFields(uniProtDataType = UniProtDataType.HELP)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {

        HelpCentreEntry entry = service.findByUniqueId(id);
        return super.getEntityResponse(entry, fields, request);
    }

    @Operation(
            summary = SEARCH_HELP_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "HelpCentreEntry",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SearchResult.class))
                        })
            })
    @GetMapping(
            value = "/search",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<HelpCentreEntry>> search(
            @Valid @ModelAttribute HelpCentreSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        searchRequest.setType(HELP_STR);
        QueryResult<HelpCentreEntry> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(HelpCentreEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            HelpCentreEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
