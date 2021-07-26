package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 26/07/2021
 */
public class ValidIdsSearchRequestTest {

    @Test
    void isValidWithDefaultReturnTrue() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void isValidWithDownloadValueTrue() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        boolean result = validator.isValid(builder.download("FaLSe").build(), null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        builder.accessions("P12345");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void inValidWithMultipleValuesReturnFalse() {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        boolean result =
                validator.isValid(
                        builder.accessions("P12345, INVALID, P54321, INVALID2, P54322").build(),
                        null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("INVALID"));
        assertTrue(validator.errorList.contains("INVALID2"));
    }

    @Test
    void inValidMaxLengthReturnFalse() {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        boolean result =
                validator.isValid(
                        builder.accessions("P10000,P20000, P30000, P40000, P50000, P60000").build(),
                        null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength"));
    }

    @Test
    void inValidMaxAndValuesLengthReturnFalse() {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        boolean result =
                validator.isValid(
                        builder.accessions("P10000,P20000, P30000, P4INVALID, P50000, P60000")
                                .build(),
                        null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength"));
        assertTrue(validator.errorList.contains("P4INVALID"));
    }

    @ParameterizedTest
    @MethodSource("provideDataTypeAndValidValues")
    void testValidIds(UniProtDataType dataType, String csv) {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator(dataType);
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        boolean result = validator.isValid(builder.accessions(csv).build(), null);
        assertTrue(result);
    }

    @ParameterizedTest
    @MethodSource("provideDataTypeAndInvalidValues")
    void testInValidIds(UniProtDataType dataType, String csv) {
        ValidIdsSearchRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsSearchRequestTest.FakeValidAccessionListValidator(dataType);
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        boolean result = validator.isValid(builder.accessions(csv).build(), null);
        assertFalse(result);
        assertEquals(3, validator.errorList.size());
        String errorString = validator.errorList.stream().collect(Collectors.joining(","));
        assertEquals(errorString, csv);
    }

    private static Stream<Arguments> provideDataTypeAndValidValues() {
        return Stream.of(
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345,P54321,P32542"),
                Arguments.of(UniProtDataType.UNIPARC, "UPI0000000001,UPI0000000002,UPI0000000003"),
                Arguments.of(
                        UniProtDataType.UNIREF,
                        "UniRef50_P12345,UniRef90_P12345,UniRef100_P12345"));
    }

    private static Stream<Arguments> provideDataTypeAndInvalidValues() {
        return Stream.of(
                Arguments.of(UniProtDataType.UNIPROTKB, "P1234,UniRef90_P12345,UPI0000000001"),
                Arguments.of(UniProtDataType.UNIPARC, "P12345,UniRef100_P12345,UPI000000000"),
                Arguments.of(UniProtDataType.UNIREF, "P12345,UPI0000000001,UniRef100P1234"));
    }

    static class FakeValidAccessionListValidator extends ValidIdsRequest.AccessionListValidator {

        final List<String> errorList = new ArrayList<>();
        final UniProtDataType dataType;

        FakeValidAccessionListValidator() {
            this(UniProtDataType.UNIPROTKB);
        }

        FakeValidAccessionListValidator(UniProtDataType dataType) {
            this.dataType = dataType;
            ValidIdsRequest validIdsRequest = getMockedValidIdsRequest();
            this.initialize(validIdsRequest);
        }

        private ValidIdsRequest getMockedValidIdsRequest() {
            ValidIdsRequest validIdsRequest = Mockito.mock(ValidIdsRequest.class);
            Mockito.when(validIdsRequest.accessions()).thenReturn("accessions");
            Mockito.when(validIdsRequest.download()).thenReturn("download");
            Mockito.when(validIdsRequest.facetFilter()).thenReturn("facetFilter");
            Mockito.when(validIdsRequest.facets()).thenReturn("facets");
            return validIdsRequest;
        }

        @Override
        void buildInvalidAccessionLengthMessage(
                ConstraintValidatorContextImpl contextImpl, int length) {
            errorList.add("invalidLength");
        }

        @Override
        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession.strip());
        }

        @Override
        int getMaxSearchLength() {
            return 5;
        }

        @Override
        UniProtDataType getDataType() {
            return this.dataType;
        }
    }
}
