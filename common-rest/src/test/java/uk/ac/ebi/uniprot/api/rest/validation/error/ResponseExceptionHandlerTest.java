package uk.ac.ebi.uniprot.api.rest.validation.error;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import uk.ac.ebi.uniprot.api.common.exception.InvalidRequestException;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author lgonzales
 */
class ResponseExceptionHandlerTest {
    private static final String REQUEST_URL = "http://localhost/test";
    private static ResponseExceptionHandler errorHandler;

    @BeforeAll
    static void setUp() {
        ErrorHandlerConfig config = new ErrorHandlerConfig();
        MessageSource messageSource = config.messageSource();
        errorHandler = new ResponseExceptionHandler(messageSource);
    }

    @Test
    void handleInternalServerErrorWithDebug() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getParameter("debugError")).thenReturn("true");
        NullPointerException causedBy = new NullPointerException("Null Pointer");
        Throwable error = new Throwable("Throwable error message", causedBy);

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .handleInternalServerError(error, request);

        //then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(2, errorMessage.getMessages().size());

        assertEquals("Internal server error", errorMessage.getMessages().get(0));
        assertEquals("Caused by: Null Pointer", errorMessage.getMessages().get(1));
    }

    @Test
    void handleBindExceptionBadRequest() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        BindException error = new BindException("target", "objectName");
        error.addError(new FieldError("objectName1", "field1", "Error With field 1"));
        error.addError(new FieldError("objectName2", "field2", "Error With field 2"));
        error.addError(new FieldError("objectName3", "field3", "Error With field 3"));


        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .handleBindExceptionBadRequest(error, request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(3, errorMessage.getMessages().size());

        assertEquals("Error With field 1", errorMessage.getMessages().get(0));
        assertEquals("Error With field 2", errorMessage.getMessages().get(1));
        assertEquals("Error With field 3", errorMessage.getMessages().get(2));
    }

    @Test
    void handleMissingServletRequestParameterExceptionBadRequest() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String param = "param";
        MissingServletRequestParameterException error = new MissingServletRequestParameterException(param, "paramType");

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .handleMissingServletRequestParameter(error, request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertThat(errorMessage.getMessages().get(0), containsString(param));
    }

    @Test
    void handleInvalidRequestExceptionBadRequest() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String message = "message describing error";
        InvalidRequestException error = new InvalidRequestException(message, null);

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .handleInvalidRequestExceptionBadRequest(error, request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertThat(errorMessage.getMessages().get(0), containsString(message));
    }

    @Test
    void constraintViolationException() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_XML_VALUE);

        Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();
        constraintViolations.add(ConstraintViolationImpl.forBeanValidation("", null,
                                                                           null, "Field Error Message", null, null, null, null, null, null, null, null));
        ConstraintViolationException error = new ConstraintViolationException(constraintViolations);


        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .constraintViolationException(error, request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_XML, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());

        assertEquals("Field Error Message", errorMessage.getMessages().get(0));
    }

    @Test
    void noHandlerFoundExceptionForXMLContentType() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_XML_VALUE);
        ResourceNotFoundException error = new ResourceNotFoundException("Error Message");

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler
                .noHandlerFoundException(error, request);

        //then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_XML, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());

        assertEquals("Resource not found", errorMessage.getMessages().get(0));
    }

}