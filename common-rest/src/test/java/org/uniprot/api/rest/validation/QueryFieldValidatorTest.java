package org.uniprot.api.rest.validation;

import org.apache.lucene.search.Query;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.store.search.field.BoostValue;
import org.uniprot.store.search.field.SearchField;
import org.uniprot.store.search.field.SearchFieldType;
import org.uniprot.store.search.field.validator.FieldValueValidator;

import javax.validation.ConstraintValidatorContext;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.validation.QueryFieldValidatorTest.FakeQueryFieldValidator.ErrorType;

/**
 * Unit Test class to validate QueryFieldValidator class behaviour
 *
 * @author lgonzales
 */
class QueryFieldValidatorTest {

    @Test
    void isValidDefaultSearchQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("P21802-2", null);
        assertTrue(result);
    }


    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("accession:P21802-2", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleAccessionWildcardQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("gene:*", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\"))", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanOrQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("((organism_id:9606) OR (gene:\"CDC7\"))", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanSubQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("((organism_id:9606) OR " +
                                                   "(gene:\"CDC7\") OR " +
                                                   "((cc_bpcp_kinetics:\"value\" AND ccev_bpcp_kinetics:\"value\")))", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanMultiSubQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("(((organism_id:9606) OR (organism_id:1234)) AND " +
                                                   "(gene:\"CDC7\") AND " +
                                                   "((cc_bpcp_kinetics:1234 AND ccev_bpcp_kinetics:\"the value\")))", null);
        assertTrue(result);
    }

    @Test
    void isValidUnsuportedBoostQueryTypeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("organism_name:human^2", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.INVALID_TYPE).size());
        assertEquals("org.apache.lucene.search.BoostQuery", validator.getErrorFields(ErrorType.INVALID_TYPE).get(0));
    }

    @Test
    void isValidInvalidOrganismNameRangeQueryFilterTypeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("organism_name:[a TO z]", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.TYPE).size());
        assertEquals("organism_name", validator.getErrorFields(ErrorType.TYPE).get(0));
    }

    @Test
    void isValidInvalidFieldNameReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("invalid:P21802", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.FIELD).size());
        assertEquals("invalid", validator.getErrorFields(ErrorType.FIELD).get(0));
    }

    @Test
    void isValidInvalidAccessionReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("accession:P21802 OR " +
                                                   "accession:invalidValue", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("accession", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidProteomeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("proteome:UP123456789 OR " +
                                                   "proteome:notProteomeId", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("proteome", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidBooleanFieldReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("active:notBoolean", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("active", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidIntegerFieldReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("taxonomy_id:notInteger", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("taxonomy_id", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    private ValidSolrQueryFields getMockedValidSolrQueryFields() {
        ValidSolrQueryFields validReturnFields = Mockito.mock(ValidSolrQueryFields.class);

        Class<? extends SearchField> returnFieldValidator = FakeSearchFields.class;
        OngoingStubbing<Class<?>> ongoingStubbing = Mockito.when(validReturnFields.fieldValidatorClazz());
        ongoingStubbing.thenReturn(returnFieldValidator);
        return validReturnFields;
    }

    /**
     * this class is responsible to fake buildErrorMessage to help tests with
     */
    static class FakeQueryFieldValidator extends ValidSolrQueryFields.QueryFieldValidator {

        enum ErrorType {
            VALUE, TYPE, FIELD, INVALID_TYPE
        }

        FakeQueryFieldValidator() {
            Arrays.stream(ErrorType.values()).forEach(errorType -> errorFields.put(errorType, new ArrayList<>()));
        }

        final Map<ErrorType, List<String>> errorFields = new HashMap<>();

        List<String> getErrorFields(ErrorType errorType) {
            return errorFields.get(errorType);
        }


        @Override
        public void addFieldValueErrorMessage(String fieldName, String value, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.VALUE).add(fieldName);
        }

        @Override
        public void addFieldTypeErrorMessage(String fieldName, SearchFieldType type, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.TYPE).add(fieldName);
        }

        @Override
        public void addFieldNameErrorMessage(String fieldName, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.FIELD).add(fieldName);
        }

        @Override
        public void addQueryTypeErrorMessage(Query inputQuery, ConstraintValidatorContext context) {
            errorFields.get(ErrorType.INVALID_TYPE).add(inputQuery.getClass().getName());
        }
    }

    /**
     * this enum is responsible to fake SearchField to help tests
     */


    enum FakeSearchFields implements SearchField {
        active(SearchFieldType.TERM, FieldValueValidator::isBooleanValue),
        accession(SearchFieldType.TERM, FieldValueValidator::isAccessionValid),
        organism_id(SearchFieldType.TERM, null),
        organism_name(SearchFieldType.TERM, null),
        taxonomy_id(SearchFieldType.TERM, FieldValueValidator::isNumberValue),
        gene(SearchFieldType.TERM, null),
        cc_bpcp_kinetics(SearchFieldType.TERM, null),
        ccev_bpcp_kinetics(SearchFieldType.TERM, null),
        proteome(SearchFieldType.TERM, FieldValueValidator::isProteomeIdValue);

        private final Predicate<String> fieldValueValidator;
        private final SearchFieldType searchFieldType;

        FakeSearchFields() {
            this.searchFieldType = null;
            this.fieldValueValidator = null;
        }

        FakeSearchFields(SearchFieldType searchFieldType, Predicate<String> fieldValueValidator) {
            this.searchFieldType = searchFieldType;
            this.fieldValueValidator = fieldValueValidator;
        }

        public Predicate<String> getFieldValueValidator() {
            return this.fieldValueValidator;
        }

        @Override
        public BoostValue getBoostValue() {
            return null;
        }

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public SearchFieldType getSearchFieldType() {
            return this.searchFieldType;
        }
    }


}
