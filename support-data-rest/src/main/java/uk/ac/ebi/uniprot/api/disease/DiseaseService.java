package uk.ac.ebi.uniprot.api.disease;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.nio.ByteBuffer;
import java.util.Optional;

@Service
public class DiseaseService {
    private static final String ACCESSION_STR = "accession";
    @Autowired
    private DiseaseRepository diseaseRepository;
    private ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    public Disease findByAccession(final String accession) {
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION_STR).is(accession.toUpperCase()));
            Optional<DiseaseDocument> optionalDoc = this.diseaseRepository.getEntry(simpleQuery);

            if (optionalDoc.isPresent()) {
                DiseaseDocument disDoc = optionalDoc.get();
                ByteBuffer diseaseBB = disDoc.getDiseaseObj();
                Disease disease = this.diseaseObjectMapper.readValue(diseaseBB.array(), Disease.class);
                return disease;
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
