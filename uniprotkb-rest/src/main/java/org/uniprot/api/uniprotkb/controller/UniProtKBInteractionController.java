package org.uniprot.api.uniprotkb.controller;

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
import org.uniprot.api.uniprotkb.model.UniProtKBEntryInteraction;
import org.uniprot.api.uniprotkb.model.UniProtKBEntryInteractions;
import org.uniprot.api.uniprotkb.service.UniProtKBEntryInteractionService;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB_INTERACTION;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Validated
@RestController
@RequestMapping(value = "/uniprotkb/accession")
public class UniProtKBInteractionController
        extends BasicSearchController<UniProtKBEntryInteractions> {

    private final UniProtKBEntryInteractionService interactionService;

    @Autowired
    protected UniProtKBInteractionController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniProtKBEntryInteractions> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            UniProtKBEntryInteractionService interactionService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB_INTERACTION);
        this.interactionService = interactionService;
    }

    @Override
    protected String getEntityId(UniProtKBEntryInteractions entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniProtKBEntryInteractions entity) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/interactions",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<MessageConverterContext<UniProtKBEntryInteractions>> getInteractions(
            @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            HttpServletRequest request) {
        UniProtKBEntryInteractions interactions = this.interactionService.getEntryInteractions(accession);

        return super.getEntityResponse(interactions, "", request);
    }
}
