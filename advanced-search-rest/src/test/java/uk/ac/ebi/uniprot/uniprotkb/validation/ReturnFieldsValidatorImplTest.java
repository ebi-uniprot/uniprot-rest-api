package uk.ac.ebi.uniprot.uniprotkb.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.uniprotkb.validation.validator.impl.UniprotReturnFieldsValidator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit Test class to validate ReturnFieldsValidator class behaviour
 *
 * @author lgonzales
 */
class ReturnFieldsValidatorImplTest {

    @Test
    void isValidNullValueReturnTrue() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();

        boolean result = validator.isValid(null,null);
        assertEquals(true,result);
    }


    @Test
    void isValiEmptyValueReturnTrue() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();

        boolean result = validator.isValid("",null);
        assertEquals(true,result);
    }

    @Test
    void isValidSingleValidFieldReturnTrue() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();

        boolean result = validator.isValid("gene_primary",null);
        assertEquals(true,result);
    }

    @Test
    void isValidSingleInvalidValidFieldReturnFalse() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();
        boolean result = validator.isValid("invalid_value",null);
        assertFalse(result);

        List<String> invalidField = validator.getErrorFields();
        assertNotNull(invalidField);
        assertTrue(invalidField.size() == 1);
        assertTrue(invalidField.get(0).equals("invalid_value"));
    }

    @Test
    void isValidMultipleValidFieldReturnTrue() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();
        boolean result = validator.isValid("gene_names,kinetics, reviewed ,accession,ft:non_ter,dr:embl",null);
        assertEquals(true,result);
    }

    @Test
    void isValidMultipleInvalidFieldReturnFalse() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();
        boolean result = validator.isValid("accession, kinetics_invalid ,reviewed_invalid,cc:rna_editing",null);
        assertFalse(result);

        List<String> invalidField = validator.getErrorFields();
        assertNotNull(invalidField);
        assertTrue(invalidField.size() == 2);
        assertTrue(invalidField.get(0).equals("kinetics_invalid"));
        assertTrue(invalidField.get(1).equals("reviewed_invalid"));
    }

    @Test
    void isValidAllCategoriesValidFieldReturnTrue() {
        FakeReturnFieldsValidatorImpl validator = new FakeReturnFieldsValidatorImpl();
        validator.fieldValidator = new UniprotReturnFieldsValidator();
        boolean result = validator.isValid("protein_name,sequence_version,ft:dna_bind,cc:pathway," +
                "keywordid,tax_id,cc:subunit,cc:induction,go_f,cc:toxic_dose,ft:top_dom,ft:peptide,3d," +
                "date_create,ft:region,tl:family,dr:dbsnp",null);
        assertTrue(result);
    }

    /**
     *  this class is responsible to fake buildErrorMessage to help tests with
     *
     */
    private static class FakeReturnFieldsValidatorImpl extends ValidReturnFields.ReturnFieldsValidatorImpl{

        List<String> errorFields = new ArrayList<>();

        @Override
        public void buildErrorMessage(String field,ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(field);
        }

        @Override
        public void disableDefaultErrorMessage(ConstraintValidatorContextImpl contextImpl) {
            //do nothing.....
        }

        List<String> getErrorFields() {
            return errorFields;
        }
    }
}