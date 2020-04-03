package org.uniprot.api.crossref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.CROSSREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.crossref.request.CrossRefSearchRequest;
import org.uniprot.api.crossref.service.CrossRefService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.UniProtDataType;

@RestController
@RequestMapping("/xref")
@Validated
public class CrossRefController extends BasicSearchController<CrossRefEntry> {
    @Autowired private CrossRefService crossRefService;
    private static final String ACCESSION_REGEX = "DB-(\\d{4})";

    public CrossRefController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory) {
        super(eventPublisher, crossrefMessageConverterContextFactory, null, CROSSREF);
    }

    @GetMapping(
            value = "/{accessionId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> findByAccession(
            @PathVariable("accessionId")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.crossref.invalid.id}")
                    String accession,
            @ValidReturnFields(uniProtDataType = UniProtDataType.CROSSREF)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        CrossRefEntry crossRefEntry = this.crossRefService.findByUniqueId(accession);

        return super.getEntityResponse(crossRefEntry, fields, request);
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> search(
            @Valid CrossRefSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<CrossRefEntry> results = this.crossRefService.search(searchRequest);

        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(CrossRefEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CrossRefEntry entity) {
        return Optional.empty();
    }
}
