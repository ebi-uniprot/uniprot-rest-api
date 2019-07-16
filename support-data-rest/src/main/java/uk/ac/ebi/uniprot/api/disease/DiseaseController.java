package uk.ac.ebi.uniprot.api.disease;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.cv.disease.Disease;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@RestController
@RequestMapping("/disease")
@Validated
public class DiseaseController {
    @Autowired
    private DiseaseService diseaseService;
    public static final String ACCESSION_REGEX = "DI-(\\d{5})";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @GetMapping(value = "/{accessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Disease getByAccession(@PathVariable("accessionId") @Pattern(regexp = ACCESSION_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message ="Invalid accession format. Expected DI-xxxxx") String accession){
        Disease disease = this.diseaseService.findByAccession(accession);
        return disease;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public QueryResult<Disease> searchCursor(@Valid DiseaseSearchRequest searchRequest,
                                                      HttpServletRequest request, HttpServletResponse response) {

        QueryResult<Disease> result = this.diseaseService.search(searchRequest);

        this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));

        return result;
    }
}