package org.uniprot.api.idmapping.request;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.request.ValidFromAndTo;

/**
 * @author sahmad
 * @created 01/03/2021
 */
class ValidFromAndToTest {
    @Test
    void testValidFromTo() {
        String from = "ACC,ID";
        String to = "SWISSPROT";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testInvalidFromToWithTaxId() {
        String from = "ACC,ID";
        String to = "SWISSPROT";
        String taxId = "taxId";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setTaxId(taxId);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertFalse(isValid);
        Assertions.assertEquals(1, validator.errorMesssage.size());
        Assertions.assertEquals("Invalid parameter 'taxId'", validator.errorMesssage.get(0));
    }

    @Test
    void testValidFromToMainRule() {
        String from = "EMBL";
        String to = "ACC";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertTrue(isValid);
        Assertions.assertTrue(validator.errorMesssage.isEmpty());
    }

    @Test
    void testValidFromToMainRuleFailure() {
        String from = "EMBL";
        String to = "NF100";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertFalse(isValid);
        Assertions.assertTrue(validator.errorMesssage.isEmpty());
    }

    @Test
    void testValidFromGeneNameToSwiss() {
        String from = "GENENAME";
        String to = "SWISSPROT";
        String taxId = "taxId";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setTaxId(taxId);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testValidFromUniParcToUniParc() {
        String from = "UPARC";
        String to = "UPARC";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertTrue(isValid);
        Assertions.assertTrue(validator.errorMesssage.isEmpty());
    }

    private ValidFromAndTo getMockedValidFromAndTo() {
        ValidFromAndTo validFromAndTo = Mockito.mock(ValidFromAndTo.class);
        Mockito.when(validFromAndTo.from()).thenReturn("from");
        Mockito.when(validFromAndTo.taxId()).thenReturn("taxId");
        Mockito.when(validFromAndTo.to()).thenReturn("to");
        return validFromAndTo;
    }

    static class FakeValidFromAndToValidator extends ValidFromAndTo.ValidFromAndToValidator {
        List<String> errorMesssage = new ArrayList<>();

        @Override
        protected void buildErrorMessage(boolean isValid, ConstraintValidatorContext context) {
            errorMesssage.add("Invalid parameter 'taxId'");
        }
    }
}
