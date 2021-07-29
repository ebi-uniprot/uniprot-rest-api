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
class ValidPostByIdsRequestTest {

    @Test
    void isValidWithSingleValueWithEmptyDownloadReturnTrue() {
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
        builder.accessions("P12345");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("'download' is a required parameter"));
    }

    @Test
    void isValidWithDownloadFalse() {
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
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
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        builder.download("true");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("'accessions' is a required parameter"));
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
        builder.accessions("P12345").download("TRUE");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueWithFieldsReturnTrue() {
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
        builder.accessions("P12345,B6J853").download("TRUE").fields("gene_names,accession");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void isValidFieldsReturnFalse() {
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
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
        FakeValidPostByIdsRequestValidator validator = new FakeValidPostByIdsRequestValidator();
        FakeIdsPostRequest.FakeIdsPostRequestBuilder builder = FakeIdsPostRequest.builder();
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

    static class FakeValidPostByIdsRequestValidator
            extends ValidPostByIdsRequest.PostByIdsRequestValidator {

        final List<String> errorList = new ArrayList<>();
        final UniProtDataType dataType;

        FakeValidPostByIdsRequestValidator() {
            this(UniProtDataType.UNIPROTKB);
        }

        FakeValidPostByIdsRequestValidator(UniProtDataType dataType) {
            this.dataType = dataType;
            ValidPostByIdsRequest validIdsRequest = getMockedValidIdsRequest();
            this.initialize(validIdsRequest);
        }

        private ValidPostByIdsRequest getMockedValidIdsRequest() {
            ValidPostByIdsRequest validIdsRequest = Mockito.mock(ValidPostByIdsRequest.class);
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
