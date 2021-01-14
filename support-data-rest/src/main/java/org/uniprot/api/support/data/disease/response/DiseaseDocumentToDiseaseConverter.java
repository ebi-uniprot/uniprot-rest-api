package org.uniprot.api.support.data.disease.response;

import java.io.IOException;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class DiseaseDocumentToDiseaseConverter implements Function<DiseaseDocument, DiseaseEntry> {
    private ObjectMapper diseaseObjectMapper =
            DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public DiseaseEntry apply(DiseaseDocument diseaseDocument) {
        DiseaseEntry disease = null;
        try {
            disease =
                    this.diseaseObjectMapper.readValue(
                            diseaseDocument.getDiseaseObj().array(), DiseaseEntry.class);
        } catch (IOException e) {
            log.error("unable to convert disease binary to disease");
        }
        return disease;
    }
}
