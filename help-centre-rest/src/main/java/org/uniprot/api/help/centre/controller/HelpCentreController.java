package org.uniprot.api.help.centre.controller;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Tag(name = "helpcentre", description = "UniProt Help centre API")
@RestController
@Validated
@RequestMapping("/helpcentre")
public class HelpCentreController extends BasicSearchController<HelpCentreEntry> {

    protected HelpCentreController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<HelpCentreEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            MessageConverterContextFactory.Resource resource) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, resource);
    }

    @Override
    protected String getEntityId(HelpCentreEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(HelpCentreEntry entity) {
        return Optional.empty();
    }
}
