package uk.ac.ebi.uniprot.api.taxonomy;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.api.taxonomy.request.TaxonomyRequestDTO;
import uk.ac.ebi.uniprot.api.taxonomy.service.TaxonomyService;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.*;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.TAXONOMY;
import static uk.ac.ebi.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

@RestController
@RequestMapping("/taxonomy")
@Validated
public class TaxonomyController {

    private final TaxonomyService taxonomyService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageConverterContextFactory<TaxonomyEntry> converterContextFactory;
    private static final String TAXONOMY_ID_REGEX = "^[0-9]+$";

    public TaxonomyController(ApplicationEventPublisher eventPublisher, TaxonomyService taxonomyService,
      MessageConverterContextFactory<TaxonomyEntry> converterContextFactory){
        this.eventPublisher = eventPublisher;
        this.taxonomyService = taxonomyService;
        this.converterContextFactory = converterContextFactory;
    }

    @GetMapping(value = "/{taxonId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TaxonomyEntry getById(@PathVariable("taxonId") @Pattern(regexp = TAXONOMY_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message ="{search.taxonomy.invalid.id}") String taxonId){
        TaxonomyEntry taxonomyEntry = this.taxonomyService.findById(Long.valueOf(taxonId));
        return taxonomyEntry;
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                    APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<TaxonomyEntry>> search(@Valid TaxonomyRequestDTO searchRequest,
                                                                               @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
                                                                               HttpServletRequest request,
                                                                               HttpServletResponse response) {
        MessageConverterContext<TaxonomyEntry> context = converterContextFactory.get(TAXONOMY, contentType);
        context.setFields(searchRequest.getFields());
        QueryResult<TaxonomyEntry> results = taxonomyService.search(searchRequest);
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList = results.getContent().stream()
                    .map(entry -> String.valueOf(entry.getTaxonId()))
                    .collect(Collectors.toList());
            context.setEntityIds(accList.stream());
        } else {
            context.setEntities(results.getContent().stream());
        }
        this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, results.getPageAndClean()));
        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
    }

}
