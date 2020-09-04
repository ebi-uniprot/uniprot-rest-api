package org.uniprot.api.rest.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 12/08/2020
 */
class ValidCommaSeparatedItemsLengthTest {

    @Test
    void testEmptyValue() {
        FakeListLengthValidator validator = new FakeListLengthValidator();
        String input = "";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testNullValue() {
        FakeListLengthValidator validator = new FakeListLengthValidator();
        String input = null;
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testSingleValue() {
        FakeListLengthValidator validator = new FakeListLengthValidator();
        String input = "first";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testMaxValue() {
        FakeListLengthValidator validator = new FakeListLengthValidator();
        String input = "first,second,third, fourth, fifth";
        boolean isValid = validator.isValid(input, null);
        assertTrue(isValid);
        assertTrue(validator.errorList.isEmpty());
    }

    @Test
    void testMoreThanMaxValue() {
        FakeListLengthValidator validator = new FakeListLengthValidator();
        String input = "first,second,third, fourth, fifth,sixth";
        boolean isValid = validator.isValid(input, null);
        assertFalse(isValid);
        assertFalse(validator.errorList.isEmpty());
    }

    static class FakeListLengthValidator
            extends ValidCommaSeparatedItemsLength.ListLengthValidator {
        private final List<String> errorList = new ArrayList<>();

        @Override
        void buildInvalidListLengthMessage(
                int totalCount, ConstraintValidatorContextImpl contextImpl) {
            errorList.add("Allowed length " + getMaxLength() + ". Passed length " + totalCount);
        }

        @Override
        int getMaxLength() {
            return 5;
        }
    }
}
