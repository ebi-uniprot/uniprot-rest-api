package org.uniprot.api.disease;

import java.io.IOException;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class DiseaseDocumentToDiseaseConverter implements Function<DiseaseDocument, Disease> {
    private ObjectMapper diseaseObjectMapper =
            DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public Disease apply(DiseaseDocument diseaseDocument) {
        Disease disease = null;
        try {
            disease =
                    this.diseaseObjectMapper.readValue(
                            diseaseDocument.getDiseaseObj().array(), Disease.class);
        } catch (IOException e) {
            log.error("unable to convert disease binary to disease");
        }
        return disease;
    }
}
