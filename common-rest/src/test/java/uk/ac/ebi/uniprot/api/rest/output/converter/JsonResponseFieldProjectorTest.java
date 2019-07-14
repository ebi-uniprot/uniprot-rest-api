package uk.ac.ebi.uniprot.api.rest.output.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.search.field.DiseaseField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sahmad
 */
public class JsonResponseFieldProjectorTest {

    private JsonResponseFieldProjector fieldProjector = new JsonResponseFieldProjector();
    private Disease disease;

    @BeforeEach
    void setUp() {
        DiseaseBuilder diseaseBuilder = new DiseaseBuilder();
        Keyword keyword = new KeywordImpl("Mental retardation", "KW-0991");
        CrossReference xref1 = new CrossReference("MIM", "617140", Arrays.asList("phenotype"));
        CrossReference xref2 = new CrossReference("MedGen", "CN238690");
        CrossReference xref3 = new CrossReference("MeSH", "D000015");
        CrossReference xref4 = new CrossReference("MeSH", "D008607");
        this.disease = diseaseBuilder.id("ZTTK syndrome")
                .accession("DI-04860")
                .acronym("ZTTKS")
                .definition("An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                .alternativeNames(Arrays.asList("Zhu-Tokita-Takenouchi-Kim syndrome", "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                .crossReferences(Arrays.asList(xref1, xref2, xref3, xref4))
                .keywords(keyword)
                .reviewedProteinCount(1L)
                .unreviewedProteinCount(0L)
                .build();

    }

    @Test
    void testProjectAllFieldsByDefault() {
        Map<String, Object> returnMap = this.fieldProjector.project(this.disease, null,
                Arrays.asList(DiseaseField.ResultFields.values()));
        Assertions.assertEquals(DiseaseField.ResultFields.values().length, returnMap.size());
        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("accession"));
        Assertions.assertNotNull(returnMap.get("accession"));
        Assertions.assertTrue(returnMap.containsKey("acronym"));
        Assertions.assertNotNull(returnMap.get("acronym"));
        Assertions.assertTrue(returnMap.containsKey("definition"));
        Assertions.assertNotNull(returnMap.get("definition"));
        Assertions.assertTrue(returnMap.containsKey("alternative_names"));
        Assertions.assertNotNull(returnMap.get("alternative_names"));
        Assertions.assertTrue(returnMap.containsKey("cross_references"));
        Assertions.assertNotNull(returnMap.get("cross_references"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));
        Assertions.assertTrue(returnMap.containsKey("reviewed_protein_count"));
        Assertions.assertNotNull(returnMap.get("reviewed_protein_count"));
        Assertions.assertTrue(returnMap.containsKey("unreviewed_protein_count"));
        Assertions.assertNotNull(returnMap.get("unreviewed_protein_count"));
    }

    @Test
    void testProjectFewValidFields(){
        List<String> returnFields = Stream.of("id", "alternative_names", "keywords").collect(Collectors.toList());
        Map<String, Object> returnMap = this.fieldProjector.project(this.disease, returnFields,
                Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size(), returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("alternative_names"));
        Assertions.assertNotNull(returnMap.get("alternative_names"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));

        Assertions.assertFalse(returnMap.containsKey("accession"));
    }

    @Test
    void testProjectFewValidOneInvalidFields(){ // it should ignore invalid fields and return only valid fields
        List<String> returnFields = Stream.of("id", "invalid_field_name", "unreviewed_protein_count").collect(Collectors.toList());
        Map<String, Object> returnMap = this.fieldProjector.project(this.disease, returnFields,
                Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size() - 1, returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("unreviewed_protein_count"));
        Assertions.assertNotNull(returnMap.get("unreviewed_protein_count"));
        Assertions.assertFalse(returnMap.containsKey("invalid_field_name"));
    }

}
