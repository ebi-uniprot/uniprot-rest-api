package org.uniprot.api.idmapping.common.request;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

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
        ConstraintValidatorContextImpl context = mock(ConstraintValidatorContextImpl.class);
        boolean isValid = validator.isValid(request, context);
        Assertions.assertFalse(isValid);
        Assertions.assertTrue(validator.errorMesssage.isEmpty());
    }

    @Test
    void testTaxIdIgnoredForOtherFromTo() {
        String from = "UniProtKB_AC-ID";
        String to = "UniProtKB-Swiss-Prot";
        String taxId = "taxId";
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setTaxId(taxId);
        FakeValidFromAndToValidator validator = new FakeValidFromAndToValidator();
        ValidFromAndTo validFromTo = getMockedValidFromAndTo();
        validator.initialize(validFromTo);
        ConstraintValidatorContextImpl context = mock(ConstraintValidatorContextImpl.class);
        boolean isValid = validator.isValid(request, context);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testValidFromGeneNameToSwiss() {
        String from = "Gene_Name";
        String to = "UniProtKB-Swiss-Prot";
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

    private static Stream<Arguments> provideInvalidFromTo() {
        return Stream.of(
                Arguments.of("INVALID", "UniProtKB-Swiss-Prot"),
                Arguments.of("UniProtKB_AC-ID", "INVALID"),
                Arguments.of("EMBL-GenBank-DDBJ_CDS", "UniRef100"),
                Arguments.of("UniProtKB-Swiss-Prot", "UniProtKB"),
                Arguments.of("UniParc", "UniProtKB_AC-ID"));
    }

    private static Stream<Arguments> provideValidFromTo() {
        return Stream.of(
                Arguments.of("UniProtKB_AC-ID", "UniProtKB-Swiss-Prot"),
                Arguments.of("EMBL-GenBank-DDBJ_CDS", "UniProtKB"),
                Arguments.of("UniParc", "UniParc"),
                Arguments.of("UniProtKB_AC-ID", "DMDM"),
                Arguments.of("Gene_Name", "UniProtKB"),
                Arguments.of("UniRef50", "UniProtKB-Swiss-Prot"),
                Arguments.of("Proteome_ID", "UniProtKB-Swiss-Prot"),
                Arguments.of("Proteome_ID", "UniProtKB"),
                Arguments.of("Proteome_ID", "UniParc"));
    }

    private ValidFromAndTo getMockedValidFromAndTo() {
        ValidFromAndTo validFromAndTo = Mockito.mock(ValidFromAndTo.class);
        Mockito.when(validFromAndTo.from()).thenReturn("from");
        Mockito.when(validFromAndTo.to()).thenReturn("to");
        return validFromAndTo;
    }

    static class FakeValidFromAndToValidator extends ValidFromAndTo.ValidFromAndToValidator {
        List<String> errorMesssage = new ArrayList<>();
    }
}
