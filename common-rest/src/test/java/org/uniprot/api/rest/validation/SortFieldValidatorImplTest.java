package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.store.config.UniProtDataType;

/**
 * Unit Test class to validate SortFieldValidatorImpl class behaviour
 *
 * @author lgonzales
 */
class SortFieldValidatorImplTest {

    @Test
    void isValidNullValueReturnTrue() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleFieldAscSortReturnTrue() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("accession asc", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleFieldDescSortReturnTrue() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("name DESC", null);
        assertTrue(result);
    }

    @Test
    void isValidMultipleFieldMultiplesCommaSpacesDescSortReturnTrue() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result =
                validator.isValid(
                        "accession desc,mnemonic DESC, name DesC , annotation_score dESc", null);
        assertTrue(result);
    }

    @Test
    void isValidMultipleFieldMultiplesCommaSpacesAscSortReturnTrue() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result =
                validator.isValid("gene asc,length asc ,mass AsC , organism_name aSc", null);
        assertTrue(result);
    }

    @Test
    void isInvalidMultipleFieldMultiplesCommaSpacesAscSortReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("gene asc,TYPO asc ,mass AsC , organism_name aSc", null);
        assertFalse(result);
    }

    @Test
    void isValidInvalidSortOrderReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("gene invalid", null);
        assertFalse(result);
        assertEquals(1, validator.errorFields.size());
    }

    @Test
    void isValidInvalidFieldNameReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("invalid asc", null);
        assertFalse(result);
        assertEquals(1, validator.errorFields.size());
    }

    @Test
    void isValidInvalidFormatReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("gene asc organism_name desc", null);
        assertFalse(result);
        assertEquals(1, validator.errorFields.size());
    }

    @Test
    void isValidInvalidMultipleSortOrderReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("gene invalid , organism_name invalid2", null);
        assertFalse(result);
        assertEquals(2, validator.errorFields.size());
        assertEquals("invalid", validator.errorFields.get(0));
        assertEquals("invalid2", validator.errorFields.get(1));
    }

    @Test
    void isValidInvalidMultipleFieldNameReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("invalid asc, invalid2 desc", null);
        assertFalse(result);
        assertEquals(2, validator.errorFields.size());
        assertEquals("invalid", validator.errorFields.get(0));
        assertEquals("invalid2", validator.errorFields.get(1));
    }

    @Test
    void isValidInvalidMultipleErrorsReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("invalidField asc ,gene invalidOrder", null);
        assertFalse(result);
        assertEquals(2, validator.errorFields.size());
        assertEquals("invalidfield", validator.errorFields.get(0));
        assertEquals("invalidorder", validator.errorFields.get(1));
    }

    private ValidSolrSortFields getMockedValidSolrQueryFields() {
        ValidSolrSortFields validSolrSortFields = Mockito.mock(ValidSolrSortFields.class);
        Mockito.when(validSolrSortFields.uniProtDataType()).thenReturn(UniProtDataType.UNIPROTKB);
        return validSolrSortFields;
    }

    /** this class is responsible to fake buildErrorMessage to help tests */
    static class FakeSortFieldValidatorImpl extends ValidSolrSortFields.SortFieldValidatorImpl {

        final List<String> errorFields = new ArrayList<>();

        @Override
        public void addInvalidSortFormatErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String value) {
            errorFields.add(value);
        }

        @Override
        public void addInvalidSortOrderErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String sortOrder) {
            errorFields.add(sortOrder);
        }

        @Override
        public void addInvalidSortFieldErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String sortField) {
            errorFields.add(sortField);
        }
    }
}
