package org.uniprot.api.idmapping.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.request.ValidFromAndTo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.ConstraintValidatorContext;

/**
 * @author sahmad
 * @created 01/03/2021
 */
class ValidFromAndToTest {
    @ParameterizedTest
    @MethodSource("provideValidFromTo")
    void testValidFromTo(String from, String to) {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        boolean isValid = validator.isValid(request, null);
        Assertions.assertTrue(isValid);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFromTo")
    void testInvalidFromTo(String from, String to) {
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

    private static Stream<Arguments> provideInvalidFromTo(){
        return Stream.of(Arguments.of("INVALID", "SWISSPROT"), Arguments.of("ACC,ID", "INVALID"),
                Arguments.of("EMBL", "NF100"), Arguments.of("SWISSPROT", "ACC"), Arguments.of("UPARC", "ACC,ID"));
    }

    private static Stream<Arguments> provideValidFromTo(){
        return Stream.of(Arguments.of("ACC,ID", "SWISSPROT"), Arguments.of("EMBL", "ACC"),
                Arguments.of("UPARC", "UPARC"), Arguments.of("ACC,ID", "DMDM_ID"),
                Arguments.of("GENENAME", "SWISSPROT"), Arguments.of("NF50", "ACC"));
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
