package uk.ac.ebi.uniprot.api.disease;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.cv.disease.Disease;

@RestController
@RequestMapping("/disease")
@Validated
public class DiseaseController {
    @Autowired
    private DiseaseService diseaseService;

    @GetMapping(value = "/accession/{accessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Disease getByAccession(@PathVariable("accessionId") String accession){
        Disease disease = this.diseaseService.findByAccession(accession);
        return disease;
    }
}