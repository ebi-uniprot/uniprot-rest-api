package org.uniprot.api.rest.validation.error;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

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
import org.uniprot.api.common.exception.*;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;
import org.uniprot.api.rest.output.job.JobSubmitResponse;

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
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getParameter("debugError")).thenReturn("true");
        NullPointerException causedBy = new NullPointerException("Null Pointer");
        Throwable error = new Throwable("Throwable error message", causedBy);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleInternalServerError(error, request, response);

        // then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertFalse(errorMessage.getMessages().isEmpty());

        assertEquals("Internal server error", errorMessage.getMessages().get(0));
        assertTrue(errorMessage.getMessages().contains("Message: Throwable error message"));
        boolean hasStackTrace =
                errorMessage.getMessages().stream().anyMatch(msg -> msg.startsWith("StackTrace: "));
        assertTrue(hasStackTrace);

        assertTrue(errorMessage.getMessages().contains("Caused by: Null Pointer"));
        boolean hasCauseStackTrace =
                errorMessage.getMessages().stream()
                        .anyMatch(msg -> msg.startsWith("Caused by StackTrace: "));
        assertTrue(hasCauseStackTrace);
    }

    @Test
    void handleBindExceptionBadRequest() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        BindException error = new BindException("target", "objectName");
        error.addError(new FieldError("objectName1", "field1", "Error With field 1"));
        error.addError(new FieldError("objectName2", "field2", "Error With field 2"));
        error.addError(new FieldError("objectName3", "field3", "Error With field 3"));

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleBindExceptionBadRequest(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(3, errorMessage.getMessages().size());

        assertEquals("Error With field 1", errorMessage.getMessages().get(0));
        assertEquals("Error With field 2", errorMessage.getMessages().get(1));
        assertEquals("Error With field 3", errorMessage.getMessages().get(2));
    }

    @Test
    void handleMissingServletRequestParameterExceptionBadRequest() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String param = "param";
        MissingServletRequestParameterException error =
                new MissingServletRequestParameterException(param, "paramType");

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleMissingServletRequestParameter(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertThat(errorMessage.getMessages().get(0), containsString(param));
    }

    @Test
    void handleInvalidRequestExceptionBadRequest() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String message = "message describing error";
        InvalidRequestException error = new InvalidRequestException(message, null);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleInvalidRequestExceptionBadRequest(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertThat(errorMessage.getMessages().get(0), containsString(message));
    }

    @Test
    void handleIllegalDownloadJobSubmissionException() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String message = "message describing error";
        String jobId = "jobId";
        IllegalDownloadJobSubmissionException error =
                new IllegalDownloadJobSubmissionException(jobId, message);

        ResponseEntity<JobSubmitResponse> responseEntity =
                errorHandler.handleIllegalDownloadJobSubmissionException(error);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        JobSubmitResponse body = responseEntity.getBody();
        assertSame(jobId, body.getJobId());
        assertEquals("%s%nsee 'status/%s' for more details".formatted(message, jobId), body.getMessage());
    }

    @Test
    void handleImportantMessageServiceExceptionInternalServerError() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String message = "message describing error";
        ImportantMessageServiceException error =
                new ImportantMessageServiceException(message, null);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleImportantMessageInternalServerError(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertThat(errorMessage.getMessages().get(0), containsString(message));
    }

    @Test
    void handleNoContentException() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        String message = "message describing error";
        NoContentException error = new NoContentException(message, null);

        ResponseEntity<Void> responseEntity =
                errorHandler.handleNoContentExceptionNoContent(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        assertNull(responseEntity.getBody());
    }

    @Test
    void constraintViolationException() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT))
                .thenReturn(MediaType.APPLICATION_XML_VALUE);

        Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();
        constraintViolations.add(
                ConstraintViolationImpl.forBeanValidation(
                        "",
                        null,
                        null,
                        "Field Error Message",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
        ConstraintViolationException error = new ConstraintViolationException(constraintViolations);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.constraintViolationException(error, request);

        // then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_XML, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());

        assertEquals("Field Error Message", errorMessage.getMessages().get(0));
    }

    @Test
    void noHandlerFoundExceptionForXMLContentType() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT))
                .thenReturn(MediaType.APPLICATION_XML_VALUE);
        ResourceNotFoundException error = new ResourceNotFoundException("Error Message");

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.noHandlerFoundException(error, request);

        // then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_XML, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());

        assertEquals("Resource not found", errorMessage.getMessages().get(0));
    }

    @Test
    void handleIllegalArgumentException() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        String message = "message describing error";
        IllegalArgumentException error = new IllegalArgumentException(message);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleIllegalArgumentException(error, request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().getMessages(), contains(message));
    }

    @Test
    void handleTooManyRequestsException() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        TooManyRequestsException error =
                new TooManyRequestsException("Too Many Requests Exception");

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleTooManyRequestsException(error, request, response);

        // then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1, errorMessage.getMessages().size());
        assertEquals(
                "Too Many Requests: We are currently experiencing a lot of API requests, and are unable to process your request at the moment. Please try again in a few minutes. If this error persists please contact us through uniprot.org/contact",
                errorMessage.getMessages().get(0));
    }

    @Test
    void handleTooManyRequestsExceptionWithDebug() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getParameter("debugError")).thenReturn("true");
        TooManyRequestsException error =
                new TooManyRequestsException("Gatekeeper did NOT let me in (space inside=0)");

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.handleTooManyRequestsException(error, request, response);

        // then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertFalse(errorMessage.getMessages().isEmpty());
        assertEquals(
                "Too Many Requests: We are currently experiencing a lot of API requests, and are unable to process your request at the moment. Please try again in a few minutes. If this error persists please contact us through uniprot.org/contact",
                errorMessage.getMessages().get(0));
        assertEquals(
                "Message: Gatekeeper did NOT let me in (space inside=0)",
                errorMessage.getMessages().get(1));
    }
}
