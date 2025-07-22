package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.ACCESSION_UNIPROTKB_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.ACCESSION_UNIPROTKB_EXAMPLE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.common.service.protlm.ProtLMUniProtKBEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(value = UNIPROTKB_RESOURCE)
@Validated
public class ProtLMUniProtKBController extends BasicSearchController<UniProtKBEntry> {

    private final ProtLMUniProtKBEntryService protLMUniProtKBEntryService;

    @Autowired
    public ProtLMUniProtKBController(
            ApplicationEventPublisher eventPublisher,
            ProtLMUniProtKBEntryService protLMUniProtKBEntryService,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.UNIPROTKB,
                downloadGatekeeper);
        this.protLMUniProtKBEntryService = protLMUniProtKBEntryService;
    }

    @GetMapping(
            value = "/protlm/{accession}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getGoogleProtLMEntryByAccession(
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
        UniProtKBEntry entry = protLMUniProtKBEntryService.getProtLMEntry(accession);
        return super.getEntityResponse(entry, null, request);
    }

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniProtKBEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
