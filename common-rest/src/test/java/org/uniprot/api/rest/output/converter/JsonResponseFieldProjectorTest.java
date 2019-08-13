package org.uniprot.api.rest.output.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.converter.JsonResponseFieldProjector;
import org.uniprot.core.builder.DiseaseBuilder;
import org.uniprot.core.cv.disease.CrossReference;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.cv.keyword.Keyword;
import org.uniprot.core.cv.keyword.impl.KeywordImpl;
import org.uniprot.core.json.parser.uniprot.*;
import org.uniprot.core.json.parser.uniprot.comment.*;
import org.uniprot.core.uniprot.ProteinExistence;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.UniProtId;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.builder.UniProtIdBuilder;
import org.uniprot.core.uniprot.comment.Comment;
import org.uniprot.store.search.field.DiseaseField;
import org.uniprot.store.search.field.UniProtField;

import java.util.*;
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
        Assertions.assertTrue(returnMap.containsKey("alternativeNames"));
        Assertions.assertNotNull(returnMap.get("alternativeNames"));
        Assertions.assertTrue(returnMap.containsKey("crossReferences"));
        Assertions.assertNotNull(returnMap.get("crossReferences"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));
        Assertions.assertTrue(returnMap.containsKey("reviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("reviewedProteinCount"));
        Assertions.assertTrue(returnMap.containsKey("unreviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("unreviewedProteinCount"));
    }

    @Test
    void testProjectFewValidFields(){
        List<String> returnFields = Stream.of("id", "alternative_names", "keywords").collect(Collectors.toList());
        Map<String, List<String>> filterFieldMap = returnFields.stream().collect(Collectors.toMap(f -> f, f -> Collections.emptyList()));

        Map<String, Object> returnMap = this.fieldProjector.project(this.disease, filterFieldMap,
                Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size(), returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("alternativeNames"));
        Assertions.assertNotNull(returnMap.get("alternativeNames"));
        Assertions.assertTrue(returnMap.containsKey("keywords"));
        Assertions.assertNotNull(returnMap.get("keywords"));

        Assertions.assertFalse(returnMap.containsKey("accession"));
    }

    @Test
    void testProjectFewValidOneInvalidFields(){ // it should ignore invalid fields and return only valid fields
        List<String> returnFields = Stream.of("id", "invalid_field_name", "unreviewed_protein_count").collect(Collectors.toList());
        Map<String, List<String>> filterFieldMap = returnFields.stream().collect(Collectors.toMap(f -> f, f -> Collections.emptyList()));

        Map<String, Object> returnMap = this.fieldProjector.project(this.disease, filterFieldMap,
                Arrays.asList(DiseaseField.ResultFields.values()));

        Assertions.assertEquals(returnFields.size() - 1, returnMap.size());

        Assertions.assertTrue(returnMap.containsKey("id"));
        Assertions.assertNotNull(returnMap.get("id"));
        Assertions.assertTrue(returnMap.containsKey("unreviewedProteinCount"));
        Assertions.assertNotNull(returnMap.get("unreviewedProteinCount"));
        Assertions.assertFalse(returnMap.containsKey("invalid_field_name"));
    }

    @Test
    void testProjectAllUniProtKBFields(){
        List<Comment> comments = new ArrayList<>();
        comments.add(AlternativeProductsCommentTest.getAlternativeProductsComment());
        comments.add(BPCPCommentTest.getBpcpComment());
        comments.add(CatalyticActivityCommentTest.getCatalyticActivityComment());
        comments.add(CofactorCommentTest.getCofactorComment());
        comments.add(DiseaseCommentTest.getDiseaseComment());
        comments.add(FreeTextCommentTest.getFreeTextComment());
        comments.add(InteractionCommentTest.getInteractionComment());
        comments.add(MassSpectrometryCommentTest.getMassSpectrometryComment());
        comments.add(RnaEditingCommentTest.getRnaEditingComment());
        comments.add(SequenceCautionCommentTest.getSequenceCautionComment());
        comments.add(SubcellularLocationCommentTest.getSubcellularLocationComment());
        comments.add(WebResourceCommentTest.getWebResourceComment());

        UniProtId uniProtId = new UniProtIdBuilder("uniprot id").build();
        UniProtEntryBuilder builder = new UniProtEntryBuilder();
        UniProtEntry entry = builder.primaryAccession(UniProtAccessionTest.getUniProtAccession())
                .uniProtId(uniProtId)
                .active()
                .entryType(UniProtEntryType.SWISSPROT)
                .addSecondaryAccession(UniProtAccessionTest.getUniProtAccession())
                .entryAudit(EntryAuditTest.getEntryAudit())
                .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                .proteinDescription(ProteinDescriptionTest.getProteinDescription())
                .genes(Collections.singletonList(GeneTest.createCompleteGene()))
                .annotationScore(2)
                .organism(OrganimsTest.getOrganism())
                .organismHosts(Collections.singletonList(OrganimHostTest.getOrganismHost()))
                .comments(comments)
                .features(Collections.singletonList(FeatureTest.getFeature()))
                .internalSection(InternalSectionTest.getInternalSection())
                .keywords(Collections.singletonList(KeywordTest.getKeyword()))
                .geneLocations(Collections.singletonList(GeneLocationTest.getGeneLocation()))
                .references(UniProtReferenceTest.getUniProtReferences())
                .databaseCrossReferences(Collections.singletonList(UniProtDBCrossReferenceTest.getUniProtDBCrossReference()))
                .sequence(SequenceTest.getSequence())
                .build();

        Map<String, List<String>> filterFieldMap = new HashMap<>();
        filterFieldMap.put("gene", Collections.EMPTY_LIST);
        filterFieldMap.put("organism", Collections.EMPTY_LIST);
        filterFieldMap.put("feature", Collections.EMPTY_LIST);
        filterFieldMap.put("xref", Collections.EMPTY_LIST);
        filterFieldMap.put("keyword", Collections.EMPTY_LIST);
        List<String> cTypes = new ArrayList<>();
        cTypes.add("disease");
        cTypes.add("webresource");
        filterFieldMap.put("comment", cTypes);
        Map<String, Object> result = this.fieldProjector.project(entry, filterFieldMap, Arrays.asList(UniProtField.ResultFields.values()));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(12, result.size(), "total number of expected fields does not match");
        Assertions.assertNotNull(result.get("genes"));
        Assertions.assertNotNull(result.get("organism"));
        Assertions.assertNotNull(result.get("features"));
        Assertions.assertNotNull(result.get("databaseCrossReferences"));
        Assertions.assertNotNull(result.get("keywords"));
        Assertions.assertNotNull(result.get("comments"));
        Assertions.assertEquals(cTypes.size(), ((List<?>) result.get("comments")).size());
        Assertions.assertEquals(entry.getEntryType(), result.get("entryType"));
        Assertions.assertEquals(entry.getPrimaryAccession(), result.get("primaryAccession"));
    }

}
