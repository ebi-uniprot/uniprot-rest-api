package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @since 29/06/2020
 */
public class ValidSolrQueryFacetFieldsTest {
    private static ValidSolrQueryFacetFieldsTest.FakeFacetValidator validator;

    @BeforeAll
    static void setUp() {
        validator = new ValidSolrQueryFacetFieldsTest.FakeFacetValidator();
    }

    @Test
    void testNullFacetFilterQuery() {
        boolean isValid = validator.isValid(null, null);
        assertTrue(isValid);
    }

    @Test
    void testSingleTermFacetFilterQuery() {
        String query = "reviewed:true";
        boolean isValid = validator.isValid(query, null);
        assertTrue(isValid);
    }

    @Test
    void testMoreThanOneFacetsFilterQuery() {
        String query = "reviewed:true AND length:[1 TO 200] AND existence:Homology";
        boolean isValid = validator.isValid(query, null);
        assertTrue(isValid);
    }

    @Test
    void testWithInvalidFacet() {
        String query = "reviewed:true AND length123:[1 TO 200] AND existence:Homology";
        boolean isValid = validator.isValid(query, null);
        assertFalse(isValid);
        assertEquals(List.of("length123"), validator.errorFields);
        validator.errorFields.clear();
    }

    @Test
    void testWithMoreThanOneInvalidFacets() {
        String query =
                "reviewed:true AND length123:[1 TO 200] AND existence:Homology OR simpleFacet:1";
        boolean isValid = validator.isValid(query, null);
        assertFalse(isValid);
        assertEquals(List.of("length123", "simpleFacet"), validator.errorFields);
        validator.errorFields.clear();
    }

    static class FakeFacetValidator extends ValidSolrQueryFacetFields.QueryFacetFieldValidator {

        final List<String> errorFields = new ArrayList<>();

        final Collection<String> mockedFacetNames =
                List.of(
                        "reviewed",
                        "fragment",
                        "structure_3d",
                        "model_organism",
                        "other_organism",
                        "existence",
                        "annotation_score",
                        "proteome",
                        "proteins_with",
                        "length");

        @Override
        void buildInvalidFacetNameMessage(
                String facetName,
                Collection<String> facetList,
                ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(facetName);
        }

        @Override
        Collection<String> getFacetNames() {
            return mockedFacetNames;
        }
    }
}
