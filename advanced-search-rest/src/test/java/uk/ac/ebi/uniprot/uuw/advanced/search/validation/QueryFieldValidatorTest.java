package uk.ac.ebi.uniprot.uuw.advanced.search.validation;

import org.apache.lucene.search.Query;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.dataservice.client.SearchFieldType;
import uk.ac.ebi.uniprot.uniprotkb.validation.validator.impl.UniprotSolrQueryFieldValidator;

import javax.validation.ConstraintValidatorContext;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.ebi.uniprot.uuw.advanced.search.validation.QueryFieldValidatorTest.FakeQueryFieldValidator.ErrorType;

/**
 * Unit Test class to validate QueryFieldValidator class behaviour
 *
 * @author lgonzales
 */
class QueryFieldValidatorTest {

    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("accession:P21802-2", null);
        assertEquals(true, result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\"))", null);
        assertEquals(true, result);
    }

    @Test
    void isValidBooleanOrQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("((organism_id:9606) OR (gene:\"CDC7\"))", null);
        assertEquals(true, result);
    }

    @Test
    void isValidBooleanSubQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("((organism_id:9606) OR " +
                                                   "(gene:\"CDC7\") OR " +
                                                   "((cc_bpcp_kinetics:\"value\" AND ccev_bpcp_kinetics:\"value\")))", null);
        assertEquals(true, result);
    }

    @Test
    void isValidBooleanMultiSubQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("(((organism_id:9606) OR (organism_id:1234)) AND " +
                                                   "(gene:\"CDC7\") AND " +
                                                   "((cc_bpcp_kinetics:1234 AND ccev_bpcp_kinetics:\"the value\")))", null);
        assertEquals(true, result);
    }

    @Test
    void isValidUnsuportedBoostQueryTypeReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("organism_name:human^2", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.INVALID_TYPE).size());
        assertEquals("org.apache.lucene.search.BoostQuery", validator.getErrorFields(ErrorType.INVALID_TYPE).get(0));
    }

    @Test
    void isValidInvalidOrganismNameRangeQueryFilterTypeReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("organism_name:[a TO z]", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.TYPE).size());
        assertEquals("organism_name", validator.getErrorFields(ErrorType.TYPE).get(0));
    }

    @Test
    void isValidInvalidFieldNameReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("invalid:P21802", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.FIELD).size());
        assertEquals("invalid", validator.getErrorFields(ErrorType.FIELD).get(0));
    }

    @Test
    void isValidInvalidAccessionReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("accession:P21802 OR " +
                                                   "accession:invalidValue", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("accession", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidProteomeReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("proteome:UP123456789 OR " +
                                                   "proteome:notProteomeId", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("proteome", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidBooleanFieldReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("active:notBoolean", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("active", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidIntegerFieldReturnFalse() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("taxonomy_id:notInteger", null);
        assertEquals(false, result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("taxonomy_id", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    /**
     * this class is responsible to fake buildErrorMessage to help tests with
     */
    static class FakeQueryFieldValidator extends uk.ac.ebi.uniprot.uuw.uniprotkb.validation.ValidSolrQueryFields.QueryFieldValidator {

        enum ErrorType {
            VALUE, TYPE, FIELD, INVALID_TYPE
        }

        FakeQueryFieldValidator() {
            Arrays.stream(ErrorType.values()).forEach(errorType -> errorFields.put(errorType, new ArrayList<>()));
        }

        Map<ErrorType, List<String>> errorFields = new HashMap<>();

        Map<ErrorType, List<String>> getErrorFields() {
            return errorFields;
        }

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

}
