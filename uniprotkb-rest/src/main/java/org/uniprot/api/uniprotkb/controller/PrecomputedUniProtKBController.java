package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.TAXONOMY_ID_REGEX;

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
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedUniProtKBEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(value = UNIPROTKB_RESOURCE)
@Validated
public class PrecomputedUniProtKBController extends BasicSearchController<UniProtKBEntry> {

    private final PrecomputedUniProtKBEntryService precomputedUniProtKBEntryService;

    @Autowired
    public PrecomputedUniProtKBController(
            ApplicationEventPublisher eventPublisher,
            PrecomputedUniProtKBEntryService precomputedUniProtKBEntryService,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.UNIPROTKB,
                downloadGatekeeper);
        this.precomputedUniProtKBEntryService = precomputedUniProtKBEntryService;
    }

    @GetMapping(
            value = "/precomputed/{upi}/{taxonId}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(hidden = true)
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getPrecomputedUniProtKBEntry(
            @PathVariable("upi")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upi.value}")
                    @Parameter(
                            description = ID_UNIPARC_DESCRIPTION,
                            example = ID_UNIPARC_EXAMPLE,
                            required = true)
                    String upi,
            @Parameter(description = ID_TAX_DESCRIPTION, example = ID_TAX_EXAMPLE, required = true)
                    @PathVariable("taxonId")
                    @Pattern(
                            regexp = TAXONOMY_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.taxonomy.invalid.id}")
                    String taxonId,
            HttpServletRequest request) {
        UniProtKBEntry entry =
                precomputedUniProtKBEntryService.getPrecomputedUniProtKBEntry(upi, taxonId);
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
