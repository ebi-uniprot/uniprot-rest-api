package uk.ac.ebi.uniprot.api.crossref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRef;
import uk.ac.ebi.uniprot.api.crossref.repository.CrossRefRepository;

import java.util.Optional;

@Service
public class CrossRefService {
    private static final String ACCESSION_STR = "accession";
    @Autowired
    private CrossRefRepository crossRefRepository;

    public CrossRef findByAccession(final String accession) {
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION_STR).is(accession.toUpperCase()));
            Optional<CrossRef> optionalDoc = crossRefRepository.getEntry(simpleQuery);

            if (optionalDoc.isPresent()) {
                return optionalDoc.get();
            } else {
                throw new ResourceNotFoundException("{search.not.found}");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

}
