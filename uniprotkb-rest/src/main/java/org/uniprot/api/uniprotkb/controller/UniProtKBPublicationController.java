package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.common.repository.model.PublicationEntry;
import org.uniprot.api.uniprotkb.common.service.publication.PublicationService;
import org.uniprot.api.uniprotkb.common.service.publication.request.PublicationRequest;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Validated
@RestController
@RequestMapping(value = "/uniprotkb")
@Tag(name = TAG_UNIPROTKB, description = TAG_UNIPROTKB_DESC)
public class UniProtKBPublicationController extends BasicSearchController<PublicationEntry> {
    private final PublicationService publicationService;

    @Autowired
    protected UniProtKBPublicationController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<PublicationEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            PublicationService publicationService) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.UNIPROTKB_PUBLICATION);
        this.publicationService = publicationService;
    }

    @Override
    protected String getEntityId(PublicationEntry entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            PublicationEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/publications",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            hidden = true,
            summary = PUBLICATION_UNIPROTKB_OPERATION,
            description = PUBLICATION_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PublicationEntry.class))
                        })
            })
    public ResponseEntity<MessageConverterContext<PublicationEntry>>
            getMappedPublicationsByUniProtAccession(
                    @Parameter(
                                    description = ACCESSION_UNIPROTKB_DESCRIPTION,
                                    example = ACCESSION_UNIPROTKB_EXAMPLE)
                            @PathVariable("accession")
                            @Pattern(
                                    regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                                    flags = {Pattern.Flag.CASE_INSENSITIVE},
                                    message = "{search.invalid.accession.value}")
                            String accession,
                    @Valid @ModelAttribute PublicationRequest publicationRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        QueryResult<PublicationEntry> literatureEntry =
                this.publicationService.getPublicationsByUniProtAccession(
                        accession, publicationRequest);

        return super.getSearchResponse(literatureEntry, "", request, response);
    }
}
