package org.uniprot.api.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        boolean result = validator.isValid("gene asc,length asc ,mass AsC , organism aSc", null);
        assertTrue(result);
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

        boolean result = validator.isValid("gene asc organism desc", null);
        assertFalse(result);
        assertEquals(1, validator.errorFields.size());
    }

    @Test
    void isValidInvalidMultipleSortOrderReturnFalse() {
        ValidSolrSortFields validSolrSortFields = getMockedValidSolrQueryFields();
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.initialize(validSolrSortFields);

        boolean result = validator.isValid("gene invalid , organism invalid2", null);
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

        Class<? extends Enum> returnFieldValidator = FakeSort.class;
        OngoingStubbing<Class<?>> ongoingStubbing =
                Mockito.when(validSolrSortFields.sortFieldEnumClazz());
        ongoingStubbing.thenReturn(returnFieldValidator);
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

    private enum FakeSort {
        accession("accession_id"),
        mnemonic("mnemonic_sort"),
        name("name_sort"),
        annotation_score("annotation_score"),
        gene("gene_sort"),
        length("length"),
        mass("mass"),
        organism("organism_sort");

        private final String solrFieldName;

        FakeSort(String solrFieldName) {
            this.solrFieldName = solrFieldName;
        }

        public String getSolrFieldName() {
            return solrFieldName;
        }

        @Override
        public String toString() {
            return this.solrFieldName;
        }
    }
}
