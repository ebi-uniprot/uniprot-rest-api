package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 26/07/2021
 */
class ValidIdsDownloadRequestTest {

    @Test
    void isValidWithSingleValueWithEmptyDownloadReturnTrue() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        builder.accessions("P12345");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("'download' is a required parameter"));
    }

    @Test
    void isValidWithDownloadFalse() {
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("tru");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(2, validator.errorList.size());
        assertTrue(validator.errorList.contains("Invalid download value"));
        assertTrue(validator.errorList.contains("'accessions' is a required parameter"));
    }

    @Test
    void isValidWithEmptyAccessionsFalse() {
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("true");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("'accessions' is a required parameter"));
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        builder.accessions("P12345").download("TRUE");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueWithFieldsReturnTrue() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        builder.accessions("P12345,B6J853").download("TRUE").fields("gene_names,accession");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void isValidFieldsReturnFalse() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        builder.accessions("P12345").download("TRUE").fields("field1,field2,field3");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(3, validator.errorList.size());
        assertTrue(
                validator.errorList.containsAll(
                        List.of(
                                "invalid field field1",
                                "invalid field field3",
                                "invalid field field3")));
    }

    @Test
    void inValidMaxLengthReturnFalse() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsDownloadRequest.FakeIdsDownloadRequestBuilder builder =
                FakeIdsDownloadRequest.builder();
        boolean result =
                validator.isValid(
                        builder.accessions("P10000,P20000, P30000, P40000, P50000, P60000")
                                .download("true")
                                .build(),
                        null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("invalidLength"));
    }

    static class FakeValidAccessionListValidator
            extends ValidDownloadByIdsRequest.AccessionListValidator {

        final List<String> errorList = new ArrayList<>();
        final UniProtDataType dataType;

        FakeValidAccessionListValidator() {
            this(UniProtDataType.UNIPROTKB);
        }

        FakeValidAccessionListValidator(UniProtDataType dataType) {
            this.dataType = dataType;
            ValidDownloadByIdsRequest validIdsRequest = getMockedValidIdsRequest();
            this.initialize(validIdsRequest);
        }

        private ValidDownloadByIdsRequest getMockedValidIdsRequest() {
            ValidDownloadByIdsRequest validIdsRequest =
                    Mockito.mock(ValidDownloadByIdsRequest.class);
            Mockito.when(validIdsRequest.accessions()).thenReturn("accessions");
            Mockito.when(validIdsRequest.download()).thenReturn("download");
            Mockito.when(validIdsRequest.fields()).thenReturn("fields");
            Mockito.when(validIdsRequest.uniProtDataType()).thenReturn(UniProtDataType.UNIPROTKB);
            return validIdsRequest;
        }

        @Override
        void buildRequiredFieldMessage(ConstraintValidatorContext context, String message) {
            errorList.add(message);
        }

        @Override
        void buildInvalidAccessionLengthMessage(
                ConstraintValidatorContextImpl contextImpl, int length) {
            errorList.add("invalidLength");
        }

        @Override
        void buildEmptyAccessionMessage(ConstraintValidatorContext context) {
            errorList.add("ids cannot be empty or null.");
        }

        @Override
        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession.strip());
        }

        @Override
        void buildErrorMessage(String field, ConstraintValidatorContextImpl contextImpl) {
            errorList.add("invalid field " + field);
        }

        @Override
        void buildInvalidDownloadValueMessage(ConstraintValidatorContext context) {
            errorList.add("Invalid download value");
        }

        @Override
        int getMaxDownloadLength() {
            return 5;
        }

        @Override
        UniProtDataType getDataType() {
            return this.dataType;
        }
    }
}
