package uk.ac.ebi.uniprot.api.disease;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.io.IOException;
import java.util.function.Function;

@Slf4j
@Service
public class DiseaseDocumentToDiseaseConverter implements Function<DiseaseDocument, Disease> {
    private ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public Disease apply(DiseaseDocument diseaseDocument) {
        Disease disease = null;
        try {
                disease = this.diseaseObjectMapper.readValue(diseaseDocument.getDiseaseObj().array(), Disease.class);
            } catch (IOException e) {
                log.error("unable to convert disease binary to disease");
            }
        return disease;
    }
}
