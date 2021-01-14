package org.uniprot.api.support.data.disease.response;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DiseaseDocumentToDiseaseConverter.class})
class DiseaseDocumentToDiseaseConverterTest {
    @Autowired private DiseaseDocumentToDiseaseConverter toDiseaseConverter;
    private final ObjectMapper diseaseObjectMapper =
            DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Test
    void shouldConvertDiseaseDocToDisease() throws JsonProcessingException {
        // create a disease object
        String id = "Sample DiseaseEntry";
        String accession = "DI-12345";
        String acronym = "SAMPLE-DIS";
        String def = "This is sample definition.";
        List<String> altNames = Arrays.asList("name1", "name2", "name3");
        Long reviwedProteinCount = 100L;
        Long unreviwedProteinCount = 200L;

        // cross ref
        List<String> props = Arrays.asList("prop1", "prop2", "prop3");
        String xrefId = "XREF-123";
        String databaseType = "SAMPLE_TYPE";
        DiseaseCrossReference cr =
                new DiseaseCrossReferenceBuilder()
                        .databaseType(databaseType)
                        .id(xrefId)
                        .propertiesSet(props)
                        .build();

        // keyword
        String kId = "Sample Keyword";
        String kwAC = "KW-1234";
        KeywordId keyword = new KeywordIdBuilder().name(kId).id(kwAC).build();

        DiseaseEntryBuilder builder = new DiseaseEntryBuilder();
        builder.name(id).id(accession).acronym(acronym).definition(def);
        builder.alternativeNamesSet(altNames).crossReferencesAdd(cr);
        builder.keywordsAdd(keyword)
                .reviewedProteinCount(reviwedProteinCount)
                .unreviewedProteinCount(unreviwedProteinCount);

        DiseaseEntry disease = builder.build();

        // convert disease to object
        byte[] diseaseObj = this.diseaseObjectMapper.writeValueAsBytes(disease);

        DiseaseDocument.DiseaseDocumentBuilder docBuilder = DiseaseDocument.builder();
        docBuilder.id(accession);
        docBuilder.diseaseObj(ByteBuffer.wrap(diseaseObj));

        DiseaseDocument diseaseDocument = docBuilder.build();
        DiseaseEntry convertedDisease = this.toDiseaseConverter.apply(diseaseDocument);

        // verify the result
        Assertions.assertEquals(disease.getName(), convertedDisease.getName());
        Assertions.assertEquals(disease.getId(), convertedDisease.getId());
        Assertions.assertEquals(disease.getAcronym(), convertedDisease.getAcronym());
        Assertions.assertEquals(disease.getDefinition(), convertedDisease.getDefinition());
        Assertions.assertEquals(
                disease.getReviewedProteinCount(), convertedDisease.getReviewedProteinCount());
        Assertions.assertEquals(
                disease.getUnreviewedProteinCount(), convertedDisease.getUnreviewedProteinCount());
        Assertions.assertEquals(
                disease.getAlternativeNames(), convertedDisease.getAlternativeNames());
        Assertions.assertEquals(
                disease.getCrossReferences(), convertedDisease.getCrossReferences());
        Assertions.assertEquals(disease.getKeywords(), convertedDisease.getKeywords());
    }
}
