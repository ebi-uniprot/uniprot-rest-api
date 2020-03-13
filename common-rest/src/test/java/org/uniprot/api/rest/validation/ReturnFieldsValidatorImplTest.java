package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.store.config.UniProtDataType;

/**
 * Unit Test class to validate ReturnFieldsValidator class behaviour
 *
 * @author lgonzales
 */
class ReturnFieldsValidatorImplTest {

    @Test
    void isValidNullValueReturnTrue() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);

        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValiEmptyValueReturnTrue() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);

        boolean result = validator.isValid("", null);
        assertTrue(result);
    }

    @Test
    void isValidSingleValidFieldReturnTrue() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);

        boolean result = validator.isValid("gene_primary", null);
        assertTrue(result);
    }

    @Test
    void isValidSingleInvalidValidFieldReturnFalse() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);
        boolean result = validator.isValid("invalid_value", null);
        assertFalse(result);

        List<String> invalidField = validator.getErrorFields();
        assertNotNull(invalidField);
        assertEquals(1, invalidField.size());
        assertEquals("invalid_value", invalidField.get(0));
    }

    @Test
    void isValidMultipleValidFieldReturnTrue() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);
        boolean result = validator.isValid("gene_names,kinetics, entry_type ,accession", null);
        assertTrue(result);
    }

    @Test
    void isValidMultipleInvalidFieldReturnFalse() {
        ValidReturnFields validReturnFields = getMockedValidReturnFields();

        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.initialize(validReturnFields);
        boolean result = validator.isValid("accession, kinetics_invalid ,reviewed_invalid", null);
        assertFalse(result);

        List<String> invalidField = validator.getErrorFields();
        assertNotNull(invalidField);
        assertEquals(2, invalidField.size());
        assertEquals("kinetics_invalid", invalidField.get(0));
        assertEquals("reviewed_invalid", invalidField.get(1));
    }

    private ValidReturnFields getMockedValidReturnFields() {
        ValidReturnFields validReturnFields = Mockito.mock(ValidReturnFields.class);
        Mockito.when(validReturnFields.uniProtDataType()).thenReturn(UniProtDataType.UNIPROTKB);
        return validReturnFields;
    }

    /** this class is responsible to fake buildErrorMessage to help tests with */
    private static class FakeReturnFieldsValidatorImpl
            extends ValidReturnFields.ReturnFieldsValidatorImpl {

        final List<String> errorFields = new ArrayList<>();

        @Override
        public void buildErrorMessage(String field, ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(field);
        }

        @Override
        public void disableDefaultErrorMessage(ConstraintValidatorContextImpl contextImpl) {
            // do nothing.....
        }

        List<String> getErrorFields() {
            return errorFields;
        }
    }
}
