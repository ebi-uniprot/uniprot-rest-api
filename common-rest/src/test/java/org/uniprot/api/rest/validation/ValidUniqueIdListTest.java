package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        boolean result = validator.isValid(csv, null);
        assertTrue(result);
    }

    @ParameterizedTest
    @MethodSource("provideDataTypeAndInvalidValues")
    void testInValidIds(UniProtDataType dataType, String csv) {
        ValidUniqueIdListTest.FakeValidAccessionListValidator validator =
                new ValidUniqueIdListTest.FakeValidAccessionListValidator(dataType);
        boolean result = validator.isValid(csv, null);
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

    static class FakeValidAccessionListValidator extends ValidUniqueIdList.AccessionListValidator {

        final List<String> errorList = new ArrayList<>();
        final UniProtDataType dataType;

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
        int getMaxLength() {
            return 5;
        }

        @Override
        UniProtDataType getDataType() {
            return this.dataType;
        }
    }
}
