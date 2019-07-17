package uk.ac.ebi.uniprot.api.crossref.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefSearchRequest;
import uk.ac.ebi.uniprot.api.crossref.service.CrossRefService;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.search.field.CrossRefField;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.CROSSREF;

@RestController
@RequestMapping("/xref")
@Validated
public class CrossRefController extends BasicSearchController<CrossRefEntry> {
    @Autowired
    private CrossRefService crossRefService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    private static final String ACCESSION_REGEX = "DB-(\\d{4})";


    public CrossRefController(ApplicationEventPublisher eventPublisher,
                              MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory) {
        super(eventPublisher, crossrefMessageConverterContextFactory, null, CROSSREF);
    }


    @GetMapping(value = "/{accessionId}", produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> findByAccession(@PathVariable("accessionId")
                                                                                  @Pattern(regexp = ACCESSION_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.crossref.invalid.id}")
                                                                                          String accession,
                                                                                  @ValidReturnFields(fieldValidatorClazz = CrossRefField.ResultFields.class)
                                                                                  @RequestParam(value = "fields", required = false)
                                                                                          String fields,
                                                                                  @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                          MediaType contentType) {

        CrossRefEntry crossRefEntry = this.crossRefService.findByAccession(accession);

        return super.getEntityResponse(crossRefEntry, fields, contentType);
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<CrossRefEntry>> search(@Valid
                                                                                 CrossRefSearchRequest searchRequest,
                                                                         @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                 MediaType contentType,
                                                                         HttpServletRequest request,
                                                                         HttpServletResponse response) {

        QueryResult<CrossRefEntry> results = this.crossRefService.search(searchRequest);

        return super.getSearchResponse(results, searchRequest.getFields(), contentType, request, response);
    }


    @Override
    protected String getEntityId(CrossRefEntry entity) {
        return entity.getAccession();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CrossRefEntry entity) {
        return Optional.empty();
    }
}