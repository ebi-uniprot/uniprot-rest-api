package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 12/08/2020
 */
class ValidEnumDisplayValueTest {

    private FakeUniParcDBListValidator validator;

    @BeforeEach
    void setupValidator() {
        validator = new FakeUniParcDBListValidator();
        validator.addValidValue("ensemblbacteria");
        validator.addValidValue("embl");
        validator.addValidValue("tremblnew");
    }

    @Test
    void testNullValue() {
        String input = null;
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
    }

    @Test
    void testEmptyValue() {
        String input = "";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testValidValue() {
        String input = "EnsemblBacteria";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testValidInvalidValues() {
        String input = "EnsemblBacteria,embl,invalid";
        boolean isValid = validator.isValid(input, null);
        assertFalse(isValid);
        assertFalse(validator.errorList.isEmpty());
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.get(0).contains("invalid"));
    }

    @Test
    void testValidValues() {
        String input = "EnsemblBacteria, EMBl, tremblnew";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testInvalidValue() {
        String input = "invalid";
        boolean isValid = validator.isValid(input, null);
        assertFalse(isValid);
        assertFalse(validator.errorList.isEmpty());
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.get(0).contains("invalid"));
    }

    static class FakeUniParcDBListValidator extends ValidEnumDisplayValue.EnumDisplayValidator {
        private final List<String> errorList = new ArrayList<>();

        @Override
        void buildInvalidUniParcDBMessage(
                String dbName, ConstraintValidatorContextImpl contextImpl) {
            errorList.add("Invalid UniParc DB Name " + dbName);
        }

        void addValidValue(String value) {
            super.getAcceptedValues().add(value);
        }
    }
}
