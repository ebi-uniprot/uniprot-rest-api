package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROT_PUBLICATION;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.service.PublicationService;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
// @Validated
// @RestController
// @RequestMapping(value = "/uniprotkb/accession")
public class UniprotKBEntryController extends BasicSearchController<PublicationEntry> {

    private final PublicationService publicationService;

    protected UniprotKBEntryController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<PublicationEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            PublicationService publicationService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROT_PUBLICATION);
        this.publicationService = publicationService;
    }

    @Override
    protected String getEntityId(PublicationEntry entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(PublicationEntry entity) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/publications",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<PublicationEntry>>
            getMappedLiteratureByUniprotAccession(
                    @PathVariable("accession")
                            @Pattern(
                                    regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                                    flags = {Pattern.Flag.CASE_INSENSITIVE},
                                    message = "{search.invalid.accession.value}")
                            String accession,
                    @Valid PublicationRequest publicationRequest,
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                            MediaType contentType,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        QueryResult<PublicationEntry> literatureEntry =
                this.publicationService.getPublicationsByUniprotAccession(
                        accession, publicationRequest);

        return super.getSearchResponse(literatureEntry, "", request, response);
    }
}
