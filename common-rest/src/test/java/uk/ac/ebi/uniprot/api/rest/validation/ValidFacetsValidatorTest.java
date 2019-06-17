package uk.ac.ebi.uniprot.api.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit Test class to validate ValidIncludeFacetsValidator class behaviour
 *
 * @author lgonzales
 */
class ValidFacetsValidatorTest {
    @Test
    void isValidNullValueReturnTrue() {
        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        boolean result = validator.isValid(null, null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleValueReturnTrue() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/json");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedFacetNames.add("validName");
        boolean result = validator.isValid("validName",null);
        assertTrue(result);
    }

    @Test
    void isValidWithMultiplesValuesAndApplicationJsonReturnTrue() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/json");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedFacetNames.add("validName");
        validator.mockedFacetNames.add("validName1");
        validator.mockedFacetNames.add("validName2");
        boolean result = validator.isValid("validName, validName1 , validName2",null);
        assertTrue(result);
    }

    @Test
    void isValidWithSingleInvalidValueReturnFalse() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/json");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        boolean result = validator.isValid("invalid",null);
        assertFalse(result);
        assertEquals(1, validator.errorFields.size());
        assertEquals("invalid", validator.errorFields.get(0));
    }

    @Test
    void isValidWithMultipleInvalidValueReturnFalse() {
        HttpServletRequest mockedServletWebRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedServletWebRequest.getHeader(Mockito.anyString())).thenReturn("application/json");

        FakeValidIncludeFacetsValidator validator = new FakeValidIncludeFacetsValidator();
        validator.mockedRequest = mockedServletWebRequest;
        validator.mockedFacetNames.add("validName");
        boolean result = validator.isValid("invalid ,validName, invalid2",null);
        assertFalse(result);
        assertEquals(2, validator.errorFields.size());
        assertEquals("invalid", validator.errorFields.get(0));
        assertEquals("invalid2", validator.errorFields.get(1));
    }

    static class FakeValidIncludeFacetsValidator extends ValidFacets.ValidIncludeFacetsValidator{

        List<String> errorFields = new ArrayList<>();

        HttpServletRequest mockedRequest;

        Collection<String> mockedFacetNames = new ArrayList<>();

        @Override
        void buildInvalidFacetNameMessage(String facetName, Collection<String> validNames, ConstraintValidatorContextImpl contextImpl) {
            errorFields.add(facetName);
        }

        @Override
        Collection<String> getFacetNames(){
            return mockedFacetNames;
        }
    }
}