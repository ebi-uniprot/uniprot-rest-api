package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;

public class ValidTSVFormatOnlyFieldsValidatorTest {

    private static final String FIELD_PATTERN = "xref_.*_full";

    @Test
    void testInitializeWithInvalidFieldPattern() {
        ValidTSVFormatOnlyFields mockedConstraing = Mockito.mock(ValidTSVFormatOnlyFields.class);
        Mockito.when(mockedConstraing.fieldPattern()).thenThrow(new RuntimeException());

        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.initialize(mockedConstraing);
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
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);
        ValidTSVFormatOnlyFields mockedConstraing = Mockito.mock(ValidTSVFormatOnlyFields.class);
        Mockito.when(mockedConstraing.fieldPattern()).thenReturn(FIELD_PATTERN);

        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.initialize(mockedConstraing);
        boolean isValid = validator.isValid(fields, null);
        assertTrue(isValid);
        assertNull(validator.errorFields);
    }

    @Test
    void testValidateExtraFieldsForJsonFormatReturnFalse() {
        String fields = "accession,xref_ensembl_full";
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);
        ValidTSVFormatOnlyFields mockedConstraing = Mockito.mock(ValidTSVFormatOnlyFields.class);
        Mockito.when(mockedConstraing.fieldPattern()).thenReturn(FIELD_PATTERN);
        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.initialize(mockedConstraing);
        boolean isValid = validator.isValid(fields, null);
        assertFalse(isValid);
        assertEquals("xref_ensembl_full", validator.errorFields);
    }

    @Test
    void testValidateMultipleExtraFieldsForJsonFormatReturnFalse() {
        String fields = "accession,xref_ensembl_full,xref_embl_full";
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        ValidTSVFormatOnlyFields mockedConstraing = Mockito.mock(ValidTSVFormatOnlyFields.class);
        Mockito.when(mockedConstraing.fieldPattern()).thenReturn(FIELD_PATTERN);

        FakeValidTSVFormatOnlyFieldsValidator validator =
                new FakeValidTSVFormatOnlyFieldsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.initialize(mockedConstraing);

        boolean isValid = validator.isValid(fields, null);
        assertFalse(isValid);
        assertEquals("xref_ensembl_full, xref_embl_full", validator.errorFields);
    }

    static class FakeValidTSVFormatOnlyFieldsValidator
            extends ValidTSVFormatOnlyFields.ValidTSVFormatOnlyFieldsValidator {

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
