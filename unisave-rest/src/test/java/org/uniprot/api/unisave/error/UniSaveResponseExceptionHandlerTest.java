package org.uniprot.api.unisave.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * Created 20/04/2020
 *
 * @author Edd
 */
class UniSaveResponseExceptionHandlerTest {
    private static final String REQUEST_URL = "http://localhost/test";
    private static UniSaveResponseExceptionHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new UniSaveResponseExceptionHandler();
    }

    @Test
    void noHandlerFoundExceptionForXMLContentType() {
        // when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT))
                .thenReturn(MediaType.APPLICATION_JSON_VALUE);
        String errorMessage = "A specific error message";
        UniSaveEntryNotFoundException error = new UniSaveEntryNotFoundException(errorMessage);

        ResponseEntity<ErrorInfo> responseEntity =
                errorHandler.noHandlerFoundException(error, request);

        // then
        assertNotNull(responseEntity);

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1, responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        assertNotNull(responseEntity.getBody());
        ErrorInfo errorInfo = responseEntity.getBody();

        assertEquals(REQUEST_URL, errorInfo.getUrl());

        assertNotNull(errorInfo.getMessages());
        assertEquals(1, errorInfo.getMessages().size());

        assertEquals(errorMessage, errorInfo.getMessages().get(0));
    }
}
