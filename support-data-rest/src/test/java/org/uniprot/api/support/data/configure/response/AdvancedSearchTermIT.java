package org.uniprot.api.support.data.configure.response;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.domain.impl.AnnotationEvidences;
import org.uniprot.store.search.domain.impl.GoEvidences;

import edu.emory.mathcs.backport.java.util.Arrays;

class AdvancedSearchTermIT {

    private static List<AdvancedSearchTerm> SEARCH_TERMS;
    private static final List<String> EXPECTED_TOP_LEVEL_TERMS =
            Arrays.asList(
                    new String[] {
                        "UniProtKB AC",
                        "Entry Name [ID]",
                        "Protein Name [DE]",
                        "Gene Name [GN]",
                        "Organism [OS]",
                        "Taxonomy [OC]",
                        "Virus host",
                        "Protein Existence [PE]",
                        "Function",
                        "Subcellular location",
                        "Pathology & Biotech",
                        "PTM/Processing",
                        "Expression",
                        "Interaction",
                        "Structure",
                        "Sequence",
                        "Family and Domains",
                        "Cross-references",
                        "Web Resources",
                        "Date Of",
                        "Gene Ontology [GO]",
                        "Keyword [KW]",
                        "Literature Citation",
                        "Proteomes",
                        "Cited for",
                        "Reviewed",
                        "Active"
                    });
    private static final List<String> FUNCTION_CHILDREN =
            Arrays.asList(
                    new String[] {
                        "Enzyme classification [EC]",
                        "Cofactors",
                        "Biophysicochemical properties",
                        "Catalytic Activity",
                        "Activity regulation",
                        "Function [CC]",
                        "Caution",
                        "Sites",
                        "Calcium binding",
                        "DNA binding",
                        "Nucleotide binding",
                        "Pathway",
                        "Miscellaneous [CC]"
                    });

    @BeforeAll
    static void setUp() {
        SEARCH_TERMS = AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.UNIPROTKB);
    }

    @ParameterizedTest(name = "[{0}] == \"{1}\" ?")
    @MethodSource("provideIndexAndLabelOfTopLevelTerms")
    void testTopLevelTermsOrder(Integer index, String label) {
        Assertions.assertEquals(
                EXPECTED_TOP_LEVEL_TERMS.size(),
                SEARCH_TERMS.size(),
                "Top level fields count does not match");
        Assertions.assertNotNull(SEARCH_TERMS.get(index));
        Assertions.assertEquals(Integer.valueOf(index), SEARCH_TERMS.get(index).getSeqNumber());
        Assertions.assertEquals(label, SEARCH_TERMS.get(index).getLabel());
    }

    @ParameterizedTest(name = "Function[{0}] == \"{1}\" ?")
    @MethodSource("provideSeqNumberAndLabelFunctionChildren")
    void testFunctionChildren(Integer childNumber, String label) {
        Assertions.assertTrue(childNumber < FUNCTION_CHILDREN.size());
        Assertions.assertNotNull(FUNCTION_CHILDREN.get(childNumber));
        Assertions.assertEquals(FUNCTION_CHILDREN.get(childNumber), label);
    }

    @Test
    void testSiblingGroupFieldHasSiblings() {
        AdvancedSearchTerm siblingGroupItem =
                SEARCH_TERMS.stream()
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("gene_ontology"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);

        Assertions.assertNotNull(siblingGroupItem);
        Assertions.assertEquals("sibling_group", siblingGroupItem.getItemType());
        Assertions.assertNotNull(siblingGroupItem.getSiblings());
        Assertions.assertEquals(2, siblingGroupItem.getSiblings().size());
        Assertions.assertNull(siblingGroupItem.getItems());
    }

    @Test
    void testGroupFieldsHasItems() {
        AdvancedSearchTerm groupItem =
                SEARCH_TERMS.stream()
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("subcellular"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);

        Assertions.assertNotNull(groupItem);
        Assertions.assertEquals("group", groupItem.getItemType());
        Assertions.assertNotNull(groupItem.getItems());
        Assertions.assertEquals(4, groupItem.getItems().size());
        Assertions.assertNull(groupItem.getSiblings());
    }

    @Test
    void testGeneOntologyEvidences() {
        AdvancedSearchTerm goEvidence =
                SEARCH_TERMS.stream()
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("gene_ontology"))
                        .flatMap(advancedSearchTerm -> advancedSearchTerm.getSiblings().stream())
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("go_evidence"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        Assertions.assertNotNull(goEvidence);
        Assertions.assertEquals("evidence", goEvidence.getFieldType());
        Assertions.assertNotNull(goEvidence.getEvidenceGroups());
        Assertions.assertEquals(
                GoEvidences.INSTANCE.getEvidences(), goEvidence.getEvidenceGroups());
    }

    @Test
    void testAnnotationEvidences() {
        AdvancedSearchTerm annotationEvidence =
                SEARCH_TERMS.stream()
                        .filter(advancedSearchTerm -> advancedSearchTerm.getId().equals("function"))
                        .flatMap(advancedSearchTerm -> advancedSearchTerm.getItems().stream())
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("cofactors"))
                        .flatMap(advancedSearchTerm -> advancedSearchTerm.getItems().stream())
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("chebi_term"))
                        .flatMap(advancedSearchTerm -> advancedSearchTerm.getSiblings().stream())
                        .filter(
                                advancedSearchTerm ->
                                        advancedSearchTerm.getId().equals("ccev_cofactor_chebi"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        Assertions.assertNotNull(annotationEvidence);
        Assertions.assertEquals("evidence", annotationEvidence.getFieldType());
        Assertions.assertNotNull(annotationEvidence.getEvidenceGroups());
        Assertions.assertEquals(
                AnnotationEvidences.INSTANCE.getEvidences(),
                annotationEvidence.getEvidenceGroups());
    }

    private static Stream<Arguments> provideIndexAndLabelOfTopLevelTerms() {
        return IntStream.range(0, EXPECTED_TOP_LEVEL_TERMS.size())
                .mapToObj(index -> Arguments.of(index, EXPECTED_TOP_LEVEL_TERMS.get(index)));
    }

    private static Stream<Arguments> provideSeqNumberAndLabelFunctionChildren() {
        Optional<AdvancedSearchTerm> optFuncTerm =
                SEARCH_TERMS.stream().filter(ft -> "Function".equals(ft.getLabel())).findFirst();
        Assertions.assertTrue(optFuncTerm.isPresent(), "Function term is not present");
        return optFuncTerm.map(ft -> ft.getItems()).get().stream()
                .map(item -> Arguments.of(item.getChildNumber(), item.getLabel()));
    }
}
