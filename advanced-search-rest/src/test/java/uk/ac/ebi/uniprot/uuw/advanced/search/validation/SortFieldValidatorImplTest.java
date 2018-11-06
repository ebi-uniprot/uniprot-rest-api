package uk.ac.ebi.uniprot.uuw.advanced.search.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Test class to validate SortFieldValidatorImpl class behaviour
 *
 * @author lgonzales
 */
class SortFieldValidatorImplTest {

    @Test
    void isValidNullValueReturnTrue() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid(null,null);
        assertEquals(true,result);
    }

    @Test
    void isValidSimpleFieldAscSortReturnTrue() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("accession asc",null);
        assertEquals(true,result);
    }

    @Test
    void isValidSimpleFieldDescSortReturnTrue() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("name DESC",null);
        assertEquals(true,result);
    }

    @Test
    void isValidMultipleFieldMultiplesCommaSpacesDescSortReturnTrue() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("accession desc,mnemonic DESC, name DesC , annotation_score dESc",null);
        assertEquals(true,result);
    }

    @Test
    void isValidMultipleFieldMultiplesCommaSpacesAscSortReturnTrue() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("gene asc,length asc ,mass AsC , organism aSc",null);
        assertEquals(true,result);
    }

    @Test
    void isValidInvalidSortOrderReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("gene invalid",null);
        assertEquals(false,result);
        assertEquals(1,validator.errorFields.size());
    }

    @Test
    void isValidInvalidFieldNameReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("invalid asc",null);
        assertEquals(false,result);
        assertEquals(1,validator.errorFields.size());
    }

    @Test
    void isValidInvalidFormatReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("gene asc organism desc",null);
        assertEquals(false,result);
        assertEquals(1,validator.errorFields.size());
    }

    @Test
    void isValidInvalidMultipleSortOrderReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("gene invalid , organism invalid2",null);
        assertEquals(false,result);
        assertEquals(2,validator.errorFields.size());
        assertEquals("invalid",validator.errorFields.get(0));
        assertEquals("invalid2",validator.errorFields.get(1));
    }

    @Test
    void isValidInvalidMultipleFieldNameReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("invalid asc, invalid2 desc",null);
        assertEquals(false,result);
        assertEquals(2,validator.errorFields.size());
        assertEquals("invalid",validator.errorFields.get(0));
        assertEquals("invalid2",validator.errorFields.get(1));
    }

    @Test
    void isValidInvalidMultipleErrorsReturnFalse() {
        FakeSortFieldValidatorImpl validator = new FakeSortFieldValidatorImpl();
        validator.valueList = Arrays.stream(UniProtField.Sort.values()).map(Enum::name).collect(Collectors.toList());
        boolean result = validator.isValid("invalidField asc ,gene invalidOrder",null);
        assertEquals(false,result);
        assertEquals(2,validator.errorFields.size());
        assertEquals("invalidfield",validator.errorFields.get(0));
        assertEquals("invalidorder",validator.errorFields.get(1));
    }

    /**
     *  this class is responsible to fake buildErrorMessage to help tests
     *
     */
    static class FakeSortFieldValidatorImpl extends ValidSolrSortFields.SortFieldValidatorImpl{

        List<String> errorFields = new ArrayList<>();

        @Override
        void addInvalidSortFormatErrorMessage(ConstraintValidatorContextImpl contextImpl, String value) {
            errorFields.add(value);
        }
        @Override
        void addInvalidSortOrderErrorMessage(ConstraintValidatorContextImpl contextImpl, String sortOrder) {
            errorFields.add(sortOrder);
        }

        @Override
        void addInvalidSortFieldErrorMessage(ConstraintValidatorContextImpl contextImpl, String sortField) {
            errorFields.add(sortField);
        }

    }

}
