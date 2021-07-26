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
public class ValidIdsDownloadRequestTest {

    @Test
    void isValidWithDownloadFalse() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("tru");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("Invalid download value"));
    }

    @Test
    void isValidWithEmptyAccessionsFalse() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("true");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("ids cannot be empty or null."));
    }

    @Test
    void isValidWithFacetsFalse() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("true").accessions("P12345").facets("facets");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("Invalid parameter 'facets'"));
    }

    @Test
    void isValidWithFacetFiterFalse() {
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        builder.download("true").accessions("P12345").facetFilter("facetFilter");
        boolean result = validator.isValid(builder.build(), null);
        assertFalse(result);
        assertNotNull(validator.errorList);
        assertEquals(1, validator.errorList.size());
        assertTrue(validator.errorList.contains("Invalid parameter 'facetFilter'"));
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
        builder.accessions("P12345").download("TRUE");
        boolean result = validator.isValid(builder.build(), null);
        assertTrue(result);
    }

    @Test
    void inValidMaxLengthReturnFalse() {
        ValidIdsDownloadRequestTest.FakeValidAccessionListValidator validator =
                new ValidIdsDownloadRequestTest.FakeValidAccessionListValidator();
        FakeIdsSearchRequest.FakeIdsSearchRequestBuilder builder = FakeIdsSearchRequest.builder();
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
        void buildEmptyAccessionMessage(ConstraintValidatorContext context) {
            errorList.add("ids cannot be empty or null.");
        }

        @Override
        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            errorList.add(accession.strip());
        }

        @Override
        void buildFaceFilterMessage(ConstraintValidatorContext context) {
            errorList.add("Invalid parameter 'facetFilter'");
        }

        @Override
        void buildFacetsMessage(ConstraintValidatorContext context) {
            errorList.add("Invalid parameter 'facets'");
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
