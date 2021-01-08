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
import org.uniprot.api.uniprotkb.model.PublicationEntry2;
import org.uniprot.api.uniprotkb.service.PublicationService2;
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
public class UniProtKBPublicationsController extends BasicSearchController<PublicationEntry2> {
    private final PublicationService2 publicationService;

    @Autowired
    protected UniProtKBPublicationsController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<PublicationEntry2> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            PublicationService2 publicationService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB_PUBLICATION);
        this.publicationService = publicationService;
    }

    @Override
    protected String getEntityId(PublicationEntry2 entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(PublicationEntry2 entity) {
        return Optional.empty();
    }

    @GetMapping(
            value = "/{accession}/publications2",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<PublicationEntry2>>
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
        QueryResult<PublicationEntry2> literatureEntry =
                this.publicationService.getPublicationsByUniProtAccession(
                        accession, publicationRequest);

        return super.getSearchResponse(literatureEntry, "", request, response);
    }
}
