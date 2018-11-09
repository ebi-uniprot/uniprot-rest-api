package uk.ac.ebi.uniprot.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *  Unit Test class to validate ValidIncludeFacetsValidator class behaviour
 *
 * @author lgonzales
 */
class ValidIncludeFacetsValidatorTest {

    @Test
    void isValidNullValueReturnTrue() {
        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        boolean result = validator.isValid(null, null);
        assertEquals(true, result);
    }

    @Test
    void isValidWithFalseValueReturnTrue() {
        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        boolean result = validator.isValid("false",null);
        assertEquals(true, result);
    }

    @Test
    void isValidWithTrueValueAndApplicationJsonReturnTrue() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/json");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        boolean result = validator.isValid("true",null);
        assertEquals(true, result);
    }

    @Test
    void isValidWithIncorrectContentTypeReturnFalse() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/xml");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        boolean result = validator.isValid("true",null);
        assertEquals(false, result);
        assertEquals(1, validator.errorFields.size());
        assertEquals("application/xml", validator.errorFields.get(0));
    }

    @Test
    void isValidWithInvalidValueReturnFalse() {
        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        boolean result = validator.isValid("invalid",null);
        assertEquals(false, result);
        assertEquals(1, validator.errorFields.size());
        assertEquals("invalid", validator.errorFields.get(0));
    }

    static class FakeValidIncludeFacetsValidator extends ValidIncludeFacets.ValidIncludeFacetsValidator{

        List<String> errorFields = new ArrayList<>();

        HttpServletRequest mockedRequest;

        @Override
        void buildUnsuportedContentTypeErrorMessage(String contentType,ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(contentType);
        }

        @Override
        void buildInvalidFormaErrorMessage(String value,ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(value);
        }

        @Override
        HttpServletRequest getRequest() {
            return this.mockedRequest;
        }
    }


}