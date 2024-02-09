package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB_INTERACTION;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.service.UniProtKBEntryInteractionService;
import org.uniprot.core.uniprotkb.interaction.InteractionEntry;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for interactions.
 *
 * <p>Created 12/05/2020
 *
 * @author Edd
 */
@Validated
@RestController
@RequestMapping(value = "/uniprotkb")
@Tag(name = TAG_UNIPROTKB, description = TAG_UNIPROTKB_DESC)
public class UniProtKBInteractionController extends BasicSearchController<InteractionEntry> {

    private final UniProtKBEntryInteractionService interactionService;

    @Autowired
    protected UniProtKBInteractionController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<InteractionEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            UniProtKBEntryInteractionService interactionService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB_INTERACTION);
        this.interactionService = interactionService;
    }

    @Override
    protected String getEntityId(InteractionEntry entity) {
        throw new UnsupportedOperationException("Get by entity ID is not supported");
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            InteractionEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/interactions",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    @Operation(
            summary = "Get interactions for a UniProtKB entry by accession.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = InteractionEntry.class))
                        })
            })
    public ResponseEntity<MessageConverterContext<InteractionEntry>> getInteractions(
            @Parameter(
                            description = ACCESSION_UNIPROTKB_DESCRIPTION,
                            example = ACCESSION_UNIPROTKB_EXAMPLE)
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            HttpServletRequest request) {
        InteractionEntry interactions = this.interactionService.getEntryInteractions(accession);

        return super.getEntityResponse(interactions, "", request);
    }
}
