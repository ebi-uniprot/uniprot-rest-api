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
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.service.PublicationService;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB_PUBLICATION;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Validated
@RestController
@RequestMapping(value = "/uniprotkb/accession")
public class UniProtKBPublicationController extends BasicSearchController<PublicationEntry> {
    private final PublicationService publicationService;

    @Autowired
    protected UniProtKBPublicationController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<PublicationEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            PublicationService publicationService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB_PUBLICATION);
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
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<PublicationEntry>>
            getMappedPublicationsByUniProtAccession(
                    @PathVariable("accession")
                            @Pattern(
                                    regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                                    flags = {Pattern.Flag.CASE_INSENSITIVE},
                                    message = "{search.invalid.accession.value}")
                            String accession,
                    @Valid PublicationRequest publicationRequest,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        QueryResult<PublicationEntry> literatureEntry =
                this.publicationService.getPublicationsByUniProtAccession(
                        accession, publicationRequest);

        return super.getSearchResponse(literatureEntry, "", request, response);
    }
}
