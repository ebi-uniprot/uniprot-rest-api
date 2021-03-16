package org.uniprot.api.rest.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author sahmad
 * @created 12/08/2020
 */
class ValidCommaSeparatedItemsLengthTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"first", "first,second,third, fourth, fifth"})
    void testEmptyValue(String input) {
        FakeListLengthValidator validator = new FakeListLengthValidator();
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
        Integer getMaxLength() {
            return 5;
        }
    }
}
