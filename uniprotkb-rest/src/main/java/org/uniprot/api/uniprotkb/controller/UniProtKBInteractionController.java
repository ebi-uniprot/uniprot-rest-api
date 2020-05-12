package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
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

/**
 * Represents a UniSaveEntry that could not be found.
 *
 * <p>Created 20/04/2020
 *
 * @author Edd
 */
@Validated
@RestController
@RequestMapping(value = "/uniprotkb/accession")
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
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(InteractionEntry entity) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/interactions",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<MessageConverterContext<InteractionEntry>> getInteractions(
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
