package org.uniprot.api.support.data.configure.response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;

import junit.framework.AssertionFailedError;

class AdvancedSearchTermIT {
    private static final String CONTEXT_PATH = "/uniprot/api";
    private static List<AdvancedSearchTerm> SEARCH_TERMS;
    private static final List<String> EXPECTED_TOP_LEVEL_TERMS =
            Arrays.asList(
                    new String[] {
                        "UniProtKB AC",
                        "Entry Name [ID]",
                        "Secondary Accession",
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
                        "Small molecule",
                        "Keyword [KW]",
                        "Literature Citation",
                        "Proteomes",
                        "Cited for",
                        "Reviewed",
                        "Active",
                        "UniRef ID",
                        "UniParc ID"
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
                        "DNA binding",
                        "Pathway",
                        "Miscellaneous [CC]"
                    });

    @BeforeAll
    static void setUp() {
        SEARCH_TERMS =
                AdvancedSearchTerm.getAdvancedSearchTerms(CONTEXT_PATH, UniProtDataType.UNIPROTKB);
    }

    @ParameterizedTest(name = "[{0}] == \"{1}\" ?")
    @MethodSource("provideIndexAndLabelOfTopLevelTerms")
    void testTopLevelTermsOrder(Integer index, String label) {
        Assertions.assertEquals(
                EXPECTED_TOP_LEVEL_TERMS.size(),
                SEARCH_TERMS.size(),
                "Top level fields count does not match");
        Assertions.assertNotNull(SEARCH_TERMS.get(index));
        Assertions.assertEquals(index, SEARCH_TERMS.get(index).getSeqNumber());
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
        Assertions.assertEquals("go", goEvidence.getTerm());
        Assertions.assertNotNull(goEvidence.getEvidenceGroups());
        List<EvidenceGroup> goEvidences = goEvidence.getEvidenceGroups();
        Assertions.assertEquals(GoEvidences.INSTANCE.getEvidences().size(), goEvidences.size());
        EvidenceGroup anyGroup = goEvidences.get(0);
        Assertions.assertEquals("Any", anyGroup.getGroupName());
        Assertions.assertNotNull(anyGroup.getItems());

        EvidenceGroup manualGroup = goEvidences.get(1);
        Assertions.assertEquals("Manual assertions", manualGroup.getGroupName());
        Assertions.assertNotNull(manualGroup.getItems());
        Assertions.assertFalse(manualGroup.getItems().isEmpty());

        EvidenceItem evidenceItem = manualGroup.getItems().get(0);
        Assertions.assertEquals("Inferred from experiment [EXP]", evidenceItem.getName());
        Assertions.assertEquals("exp", evidenceItem.getCode());
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
