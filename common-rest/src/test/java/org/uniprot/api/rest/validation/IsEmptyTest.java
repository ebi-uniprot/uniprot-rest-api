package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IsEmptyTest {

    @Test
    void isValidNullValueReturnFalse() {
        FakeIsEmptyTest validator = new FakeIsEmptyTest();
        boolean result = validator.isValid(null, null);
        assertFalse(result);
    }

    @Test
    void isValidWithValueReturnFalse() {
        FakeIsEmptyTest validator = new FakeIsEmptyTest();
        boolean result = validator.isValid("WithValueIsFalse", null);
        assertFalse(result);
    }

    @Test
    void isValidEmptyValueReturnTrue() {
        FakeIsEmptyTest validator = new FakeIsEmptyTest();
        boolean result = validator.isValid("", null);
        assertTrue(result);
    }

    static class FakeIsEmptyTest extends IsEmpty.IsEmptyValidator {}
}
