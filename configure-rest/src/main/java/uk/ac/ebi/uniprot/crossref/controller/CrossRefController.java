package uk.ac.ebi.uniprot.crossref.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.crossref.model.CrossRef;
import uk.ac.ebi.uniprot.crossref.service.CrossRefService;

@RestController
@RequestMapping("/v1/xref")
public class CrossRefController {
    @Autowired
    private CrossRefService crossRefService;

    @GetMapping(value = "/accession/{accessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CrossRef getByAccession(@PathVariable("accessionId") String accession){
        CrossRef crossRef = crossRefService.findByAccession(accession);
        return crossRef;
    }
}
