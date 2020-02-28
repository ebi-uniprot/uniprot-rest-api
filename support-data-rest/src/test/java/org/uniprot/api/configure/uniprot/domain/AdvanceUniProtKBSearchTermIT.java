package org.uniprot.api.configure.uniprot.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.configure.uniprot.domain.model.AdvanceUniProtKBSearchTerm;

import edu.emory.mathcs.backport.java.util.Arrays;

public class AdvanceUniProtKBSearchTermIT {

    private static List<AdvanceUniProtKBSearchTerm> SEARCH_TERMS;
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
        SEARCH_TERMS = AdvanceUniProtKBSearchTerm.getUniProtKBSearchTerms();
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

    private static Stream<Arguments> provideIndexAndLabelOfTopLevelTerms() {
        return IntStream.range(0, EXPECTED_TOP_LEVEL_TERMS.size())
                .mapToObj(index -> Arguments.of(index, EXPECTED_TOP_LEVEL_TERMS.get(index)));
    }

    private static Stream<Arguments> provideSeqNumberAndLabelFunctionChildren() {
        Optional<AdvanceUniProtKBSearchTerm> optFuncTerm =
                SEARCH_TERMS.stream().filter(ft -> "Function".equals(ft.getLabel())).findFirst();
        Assertions.assertTrue(optFuncTerm.isPresent(), "Function term is not present");
        return optFuncTerm.map(ft -> ft.getItems()).get().stream()
                .map(item -> Arguments.of(item.getChildNumber(), item.getLabel()));
    }
}
