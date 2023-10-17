package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;

class ValidTSVAndXLSFormatOnlyFieldsValidatorTest {

    private static final String FIELD_PATTERN = "xref_.*_full";

    @Test
    void testInitializeWithInvalidFieldPattern() {
        ValidTSVAndXLSFormatOnlyFields mockedConstraint =
                Mockito.mock(ValidTSVAndXLSFormatOnlyFields.class);
        Mockito.when(mockedConstraint.fieldPattern()).thenThrow(new RuntimeException());

        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.initialize(mockedConstraint);
        assertTrue(validator.getFieldPattern().isEmpty());
    }

    @Test
    void testNullFieldValueReturnTrue() {
        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        boolean isValid = validator.isValid(null, null);
        assertTrue(isValid);
    }

    @Test
    void testValidateExtraFieldsForTSVFormatReturnTrue() {
        String fields = "accession,xref_ensembl_full";
        String contentType = UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
        validateSuccess(fields, contentType);
    }

    @Test
    void testValidateExtraFieldsForXLSFormatReturnTrue() {
        String fields = "accession,xref_ensembl_full";
        String contentType = UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
        validateSuccess(fields, contentType);
    }

    @Test
    void testValidateNotExtraFieldsForJsonFormatReturnTrue() {
        String fields = "accession,xref_ensembl";
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        validateSuccess(fields, contentType);
    }

    @Test
    void testValidateExtraFieldsForJsonFormatReturnFalse() {
        String fields = "accession,xref_ensembl_full";
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        validateInvalid(fields, contentType, "xref_ensembl_full");
    }

    @Test
    void testValidateMultipleExtraFieldsForJsonFormatReturnFalse() {
        String fields = "accession,xref_ensembl_full,xref_embl_full";
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        validateInvalid(fields, contentType, "xref_ensembl_full, xref_embl_full");
    }

    private void validateSuccess(String fields, String contentType) {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);
        ValidTSVAndXLSFormatOnlyFields mockedConstraint =
                Mockito.mock(ValidTSVAndXLSFormatOnlyFields.class);
        Mockito.when(mockedConstraint.fieldPattern()).thenReturn(FIELD_PATTERN);

        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.initialize(mockedConstraint);
        boolean isValid = validator.isValid(fields, null);
        assertTrue(isValid);
        assertNull(validator.errorFields);
    }

    private void validateInvalid(String fields, String contentType, String expectedInvalidInput) {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);
        ValidTSVAndXLSFormatOnlyFields mockedConstraint =
                Mockito.mock(ValidTSVAndXLSFormatOnlyFields.class);
        Mockito.when(mockedConstraint.fieldPattern()).thenReturn(FIELD_PATTERN);
        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.initialize(mockedConstraint);
        boolean isValid = validator.isValid(fields, null);
        assertFalse(isValid);
        assertEquals(expectedInvalidInput, validator.errorFields);
    }

    static class FakeValidTSVFormatOnlyFieldsValidator
            extends ValidTSVAndXLSFormatOnlyFields.ValidTSVAndXLSFormatOnlyFieldsValidator {

        String errorFields;

        HttpServletRequest mockedRequest;

        @Override
        HttpServletRequest getRequest() {
            return this.mockedRequest;
        }

        @Override
        void buildUnsupportedContentTypeErrorMessage(
                String invalidFields, ConstraintValidatorContextImpl contextImpl) {
            errorFields = invalidFields;
        }
    }
}
