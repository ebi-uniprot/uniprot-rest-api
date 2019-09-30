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

/**
 * Unit Test class to validate the behaviour of {@link
 * org.uniprot.api.rest.validation.ValidContentTypes}
 *
 * @author Edd
 */
class ContentTypeValidatorTest {
    @Test
    void isValidNullValueReturnTrue() {
        FakeContentTypeValidator validator = new FakeContentTypeValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidWithCorrectContentType() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeContentTypeValidator validator = new FakeContentTypeValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedValidContentTypes.add(contentType);
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isNotValidWithOneContentType() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeContentTypeValidator validator = new FakeContentTypeValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedValidContentTypes.add(contentType + "THIS RUINS THE CONTENT TYPE");
        boolean result = validator.isValid(contentType, null);
        assertFalse(result);
        assertThat(validator.errorFields, contains(contentType));
    }

    @Test
    void isValidWithMultipleContentTypes() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeContentTypeValidator validator = new FakeContentTypeValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedValidContentTypes.add(contentType);
        validator.mockedValidContentTypes.add("application/xml");
        validator.mockedValidContentTypes.add("text/plain");
        boolean result = validator.isValid(contentType, null);
        assertTrue(result);
    }

    @Test
    void isNotValidWithMultipleContentTypes() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        String contentType = "application/json";
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString()))
                .thenReturn(contentType);

        FakeContentTypeValidator validator = new FakeContentTypeValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedValidContentTypes.add(contentType + "THIS RUINS THE CONTENT TYPE");
        validator.mockedValidContentTypes.add("application/xml");
        validator.mockedValidContentTypes.add("text/plain");
        boolean result = validator.isValid(contentType, null);
        assertFalse(result);
        assertThat(validator.errorFields, contains(contentType));
    }

    static class FakeContentTypeValidator extends ValidContentTypes.ContentTypesValidator {
        final List<String> errorFields = new ArrayList<>();

        HttpServletRequest mockedRequest;

        final Collection<String> mockedValidContentTypes = new ArrayList<>();

        @Override
        void buildUnsupportedContentTypeErrorMessage(
                String contentType, ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(contentType);
        }

        @Override
        HttpServletRequest getRequest() {
            return this.mockedRequest;
        }

        @Override
        Collection<String> getContentTypes() {
            return this.mockedValidContentTypes;
        }
    }
}
