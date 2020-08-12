package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 12/08/2020
 */
public class ValidUniParcDatabaseListTest {

    @Test
    void testNullValue() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = null;
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
    }

    @Test
    void testEmptyValue() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = "";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testValidValue() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = "EnsemblBacteria";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testValidInvalidValues() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = "EnsemblBacteria,embl,invalid";
        boolean isValid = validator.isValid(input, null);
        assertFalse(isValid);
        assertFalse(validator.errorList.isEmpty());
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.get(0).contains("invalid"));
    }

    @Test
    void testValidValues() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = "EnsemblBacteria, EMBl, tremblnew";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testInvalidValue() {
        FakeUniParcDBListValidator validator = new FakeUniParcDBListValidator();
        String input = "invalid";
        boolean isValid = validator.isValid(input, null);
        assertFalse(isValid);
        assertFalse(validator.errorList.isEmpty());
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.get(0).contains("invalid"));
    }

    static class FakeUniParcDBListValidator
            extends ValidUniParcDatabaseList.UniParcDatabaseListValidator {
        private final List<String> errorList = new ArrayList<>();

        @Override
        void buildInvalidUniParcDBMessage(
                String dbName, ConstraintValidatorContextImpl contextImpl) {
            errorList.add("Invalid UniParc DB Name " + dbName);
        }
    }
}
