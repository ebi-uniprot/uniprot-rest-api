package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.uniprotkb.controller.PrecomputedUniProtKBController.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.TAXONOMY_ID_REGEX;

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
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedAnnotationSearchByProteomeRequest;
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedUniProtKBEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(value = PRECOMPUTED_ANNOTATION_RESOURCE)
@Validated
public class PrecomputedUniProtKBController extends BasicSearchController<UniProtKBEntry> {
    static final String PRECOMPUTED_ANNOTATION_RESOURCE = UNIPROTKB_RESOURCE + "/precomputed";

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
            value = "/{upi}/{taxonId}",
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

    @GetMapping(
            value = "/proteome/{upId}",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(hidden = true)
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> searchByProteomeId(
            @Valid @ModelAttribute PrecomputedAnnotationSearchByProteomeRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        setBasicRequestFormat(searchRequest, request);
        QueryResult<UniProtKBEntry> results =
                precomputedUniProtKBEntryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
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
