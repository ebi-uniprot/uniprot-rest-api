package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.request.FakeDownloadRequest;

class ValidDownloadRequestTest {

    @Test
    void when_fields_passed_with_supported_format_then_valid_request() {
        FakeDownloadRequest downloadRequest = new FakeDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setFields("field1,field2");
        FakeDownloadRequestValidator validator = new FakeDownloadRequestValidator();
        boolean result = validator.isValid(downloadRequest, null);
        assertTrue(result);
        assertNull(validator.errorMessage);
    }

    @Test
    void when_fields_passed_with_unsupported_format_then_invalid_request() {
        FakeDownloadRequest downloadRequest = new FakeDownloadRequest();
        downloadRequest.setFormat(
                ValidDownloadRequest.FORMATS_WITH_NO_PROJECTION.stream().findFirst().get());
        downloadRequest.setFields("field1,field2");
        FakeDownloadRequestValidator validator = new FakeDownloadRequestValidator();
        boolean result = validator.isValid(downloadRequest, null);
        assertFalse(result);
        assertNotNull(validator.errorMessage);
    }

    @Test
    void when_fields_null_then_valid_request() {
        FakeDownloadRequest downloadRequest = new FakeDownloadRequest();
        downloadRequest.setFormat(
                ValidDownloadRequest.FORMATS_WITH_NO_PROJECTION.stream().findFirst().get());
        downloadRequest.setFields(null);
        FakeDownloadRequestValidator validator = new FakeDownloadRequestValidator();
        boolean result = validator.isValid(downloadRequest, null);
        assertTrue(result);
        assertNull(validator.errorMessage);
    }

    @Test
    void when_fields_empty_then_valid_request() {
        FakeDownloadRequest downloadRequest = new FakeDownloadRequest();
        downloadRequest.setFormat(
                ValidDownloadRequest.FORMATS_WITH_NO_PROJECTION.stream().findFirst().get());
        downloadRequest.setFields("");
        FakeDownloadRequestValidator validator = new FakeDownloadRequestValidator();
        boolean result = validator.isValid(downloadRequest, null);
        assertTrue(result);
        assertNull(validator.errorMessage);
    }

    @Test
    void when_fields_passed_with_short_format_then_invalid_request() {
        FakeDownloadRequest downloadRequest = new FakeDownloadRequest();
        downloadRequest.setFormat("h5");
        downloadRequest.setFields("field1,field2");
        FakeDownloadRequestValidator validator = new FakeDownloadRequestValidator();
        boolean result = validator.isValid(downloadRequest, null);
        assertFalse(result);
        assertNotNull(validator.errorMessage);
    }

    class FakeDownloadRequestValidator extends ValidDownloadRequest.DownloadRequestValidator {
        String errorMessage;

        @Override
        void buildInvalidParamFieldsErrorMessage(
                String format, ConstraintValidatorContextImpl contextImpl) {
            this.errorMessage = format;
        }
    }
}
