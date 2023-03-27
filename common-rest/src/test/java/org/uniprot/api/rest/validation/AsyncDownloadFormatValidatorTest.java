package org.uniprot.api.rest.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * Unit Test class to validate the behaviour of {@link ValidAsyncDownloadFormats}
 *
 * @author sahmad
 */
class AsyncDownloadFormatValidatorTest {
    @Test
    void isValidNullValueReturnTrue() {
        FakeFormatValidator validator = new FakeFormatValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidWithCorrectFormat() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(contentType);
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isNotValidWithOneFormat() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(contentType + "THIS RUINS THE CONTENT TYPE");
        boolean result = validator.isValid(contentType, null);
        assertFalse(result);
        assertThat(validator.errorFields, contains(contentType));
    }

    @Test
    void isValidWithMultipleFormats() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(contentType);
        validator.mockedValidFormats.add("application/xml");
        validator.mockedValidFormats.add("text/plain");
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isNotValidWithMultipleFormats() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(contentType + "THIS RUINS THE CONTENT TYPE");
        validator.mockedValidFormats.add("application/xml");
        validator.mockedValidFormats.add("text/plain");
        boolean result = validator.isValid(contentType, null);
        assertFalse(result);
        assertThat(validator.errorFields, contains(contentType));
    }

    @Test
    void isValidWithCorrectShortFormat() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "fasta";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isInvalidWithCorrectShortFormat() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "markdown";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(contentType, null);
        assertFalse(result);
    }

    @Test
    void isValidWithShortH5Format() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "h5";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(UniProtMediaType.HDF5_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isValidWithFullH5Format() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/x-hdf5";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeFormatValidator validator = new FakeFormatValidator();
        validator.mockedValidFormats.add(UniProtMediaType.HDF5_MEDIA_TYPE_VALUE);
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    static class FakeFormatValidator extends ValidAsyncDownloadFormats.FormatsValidator {
        final List<String> errorFields = new ArrayList<>();

        final Collection<String> mockedValidFormats = new ArrayList<>();

        @Override
        void buildUnsupportedFormatErrorMessage(
                String contentType, ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(contentType);
        }

        @Override
        Collection<String> getFormats() {
            return this.mockedValidFormats;
        }
    }
}
