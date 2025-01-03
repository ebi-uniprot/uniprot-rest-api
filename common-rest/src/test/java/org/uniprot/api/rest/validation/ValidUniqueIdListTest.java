package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author lgonzales
 * @since 23/06/2020
 */
class ValidUniqueIdListTest {

    @Test
    void isValidNullValueReturnTrue() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P12345", null);
        assertTrue(result);
    }

    @Test
    void inValidWithMultipleValuesReturnFalse() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P12345, INVALID, P54321, INVALID2, P54322", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("INVALID"));
        assertTrue(validator.errorList.contains("INVALID2"));
    }

    @Test
    void inValidMaxLengthReturnFalse() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator();

        boolean result = validator.isValid("P10000,P20000, P30000, P40000, P50000, P60000", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength. Max allowed length 5"));
    }

    @Test
    void inValidMaxAndValuesLengthReturnFalse() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator();

        boolean result =
                validator.isValid("P10000,P20000, P30000, P4INVALID, P50000, P60000", null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength. Max allowed length 5"));
        assertTrue(validator.errorList.contains("P4INVALID"));
    }

    @ParameterizedTest
    @MethodSource("provideDataTypeAndValidValues")
    void testValidIds(UniProtDataType dataType, String csv) {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(dataType);
        Mockito.when(validator.getHttpServletRequest().getHeader("Accept"))
                .thenReturn(UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(csv, null);
        assertTrue(result);
    }

    @ParameterizedTest
    @MethodSource("provideDataTypeAndInvalidValues")
    void testInvalidIds(UniProtDataType dataType, String csv) {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(dataType);
        Mockito.when(validator.getHttpServletRequest().getHeader("Accept"))
                .thenReturn(UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(csv, null);
        assertFalse(result);
        assertEquals(5, validator.errorList.size());
        String errorString = validator.errorList.stream().collect(Collectors.joining(","));
        assertEquals(errorString, csv);
    }

    @Test
    void testIsValidFormatForSubsequenceSuccess() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(
                        UniProtDataType.UNIPROTKB);
        boolean result =
                validator.isValidFormatForSubsequence(
                        "P12345[10-20]",
                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                        validator.dataType);
        assertTrue(result);
    }

    @Test
    void testIsValidFormatForSubsequenceFailure() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(
                        UniProtDataType.UNIPROTKB);
        boolean result =
                validator.isValidFormatForSubsequence(
                        "P12345[10-20]", MediaType.APPLICATION_JSON_VALUE, validator.dataType);
        assertFalse(result);
    }

    @Test
    void testIsValidSequenceRangeSuccess() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(
                        UniProtDataType.UNIPROTKB);
        assertTrue(validator.isValidSequenceRange("P12345[10-20]", validator.dataType));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidSequenceRange")
    void testInValidIds(UniProtDataType dataType, String accessionWithRange) {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(dataType);
        boolean result = validator.isValidSequenceRange(accessionWithRange, validator.dataType);
        assertFalse(result);
    }

    @Test
    void testIsValidUniParcSequenceRangeSuccess() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(UniProtDataType.UNIPARC);
        assertTrue(validator.isValidSequenceRange("UPI0000000001[10-20]", validator.dataType));
    }

    @Test
    void testInvalidFormatForUniParcSubsequenceFailure() {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(UniProtDataType.UNIPARC);
        boolean result =
                validator.isValidFormatForSubsequence(
                        "UPI0000000001[10-20]",
                        MediaType.APPLICATION_JSON_VALUE,
                        validator.dataType);
        assertFalse(result);
    }

    private static Stream<Arguments> provideDataTypeAndValidValues() {
        return Stream.of(
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345,P54321,P32542,P12345[10-20]"),
                Arguments.of(
                        UniProtDataType.UNIPARC,
                        "UPI0000000001,UPI0000000002[10-20],UPI0000000003"),
                Arguments.of(
                        UniProtDataType.UNIREF,
                        "UniRef50_P12345,UniRef90_P12345,UniRef100_P12345"));
    }

    private static Stream<Arguments> provideDataTypeAndInvalidValues() {
        return Stream.of(
                Arguments.of(
                        UniProtDataType.UNIPROTKB,
                        "P12345[0-,P12345[0-10],P1234,UniRef90_P12345,UPI0000000001"),
                Arguments.of(
                        UniProtDataType.UNIPARC,
                        "P12345,UniRef100_P12345,UPI000000000,UPI000000010,UPI0000000001[20-10]"),
                Arguments.of(
                        UniProtDataType.UNIREF,
                        "P12345,UPI0000000001,UniRef100P1234,P12345[10-20],UniRef100P1234[20-30]"));
    }

    private static Stream<Arguments> provideInvalidSequenceRange() {
        return Stream.of(
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345[0-10]"),
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345[1-4147483647]"),
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345[4147483647-40]"),
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345[4147483647-5147483647]"),
                Arguments.of(UniProtDataType.UNIPROTKB, "P12345[20-10]"),
                Arguments.of(UniProtDataType.UNIPARC, "UPI0000000001[20-10]"),
                Arguments.of(UniProtDataType.UNIPARC, "UPI0000000001[0-10]"));
    }

    static class FakeValidAccessionListValidator extends ValidUniqueIdList.AccessionListValidator {

        final List<String> errorList = new ArrayList<>();
        final UniProtDataType dataType;
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        FakeValidAccessionListValidator() {
            this(UniProtDataType.UNIPROTKB);
        }

        FakeValidAccessionListValidator(UniProtDataType dataType) {
            this.dataType = dataType;
        }

        @Override
        void buildInvalidAccessionLengthMessage(
                ConstraintValidatorContextImpl contextImpl, int length) {
            errorList.add("invalidLength. " + "Max allowed length " + length);
        }

        @Override
        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession.strip());
        }

        @Override
        void invalidSequeceRangeMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession.strip());
        }

        @Override
        void buildInvalidFormatErrorMessage(
                String format, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(format);
        }

        @Override
        int getMaxLength() {
            return 5;
        }

        @Override
        UniProtDataType getDataType() {
            return this.dataType;
        }

        @Override
        HttpServletRequest getHttpServletRequest() {
            return request;
        }
    }
}
