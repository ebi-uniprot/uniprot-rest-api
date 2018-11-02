package uk.ac.ebi.uniprot.uuw.advanced.search.validation;

import org.apache.lucene.search.Query;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.dataservice.client.SearchFieldType;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.impl.UniprotSolrQueryFieldValidator;

import javax.validation.ConstraintValidatorContext;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *  Unit Test class to validate QueryFieldValidator class behaviour
 *
 * @author lgonzales
 */
class QueryFieldValidatorTest {


    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("accession:P21802",null);
        assertEquals(true,result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("(organism_id:9606) AND (gene:\"CDC7\")",null);
        assertEquals(true,result);
    }

    @Test
    void isValidBooleanOrQueryReturnTrue() {
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.fieldValidator = new UniprotSolrQueryFieldValidator();

        boolean result = validator.isValid("(organism_id:9606) OR (gene:\"CDC7\")",null);
        assertEquals(true,result);
    }

    /**
     *  this class is responsible to fake buildErrorMessage to help tests with
     *
     */
    private static class FakeQueryFieldValidator extends ValidSolrQueryFields.QueryFieldValidator{

        enum ErrorType{
            VALUE,TYPE,FIELD,INVALID_TYPE
        }

        FakeQueryFieldValidator(){
            Arrays.stream(ErrorType.values()).forEach(errorType -> errorFields.put(errorType,new ArrayList<>()));
        }

        Map<ErrorType,List<String>> errorFields = new HashMap<>();

        Map<ErrorType,List<String>> getErrorFields() {
            return errorFields;
        }

        List<String> getErrorFields(ErrorType errorType) {
            return errorFields.get(errorType);
        }


        @Override
        void addFieldValueErrorMessage(String fieldName, String value, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.VALUE).add(fieldName);
        }

        @Override
        void addFieldTypeErrorMessage(String fieldName, SearchFieldType type, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.TYPE).add(fieldName);
        }

        @Override
        void addFieldNameErrorMessage(String fieldName, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.FIELD).add(fieldName);
        }

        @Override
        void addQueryTypeErrorMessage(Query inputQuery, ConstraintValidatorContext context) {
            errorFields.get(ErrorType.INVALID_TYPE).add(inputQuery.getClass().getName());
        }
    }

}
