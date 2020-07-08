package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 23/06/2020
 */
public class ValidAccessionListTest {

    @Test
    void isValidNullValueReturnTrue() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P12345", null);
        assertTrue(result);
    }

    @Test
    void isValidWithMultipleValueReturnTrue() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P12345, P54321, P32542", null);
        assertTrue(result);
    }

    @Test
    void inValidWithMultipleValuesReturnFalse() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P12345, INVALID, P54321, INVALID2, P54322", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("INVALID"));
        assertTrue(validator.errorList.contains("INVALID2"));
    }

    @Test
    void inValidMaxLengthReturnFalse() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P10000,P20000, P30000, P40000, P50000, P60000", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength"));
    }

    @Test
    void inValidMaxAndValuesLengthReturnFalse() {
        ValidAccessionListTest.FakeValidAccessionListValidator validator =
                new ValidAccessionListTest.FakeValidAccessionListValidator();

        boolean result =
                validator.isValid("P10000,P20000, P30000, P4INVALID, P50000, P60000", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength"));
        assertTrue(validator.errorList.contains("P4INVALID"));
    }

    static class FakeValidAccessionListValidator extends ValidAccessionList.AccessionListValidator {

        final List<String> errorList = new ArrayList<>();

        @Override
        void buildInvalidAccessionLengthMessage(ConstraintValidatorContextImpl contextImpl) {
            errorList.add("invalidLength");
        }

        @Override
        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession);
        }

        @Override
        int getMaxLength() {
            return 5;
        }
    }
}
