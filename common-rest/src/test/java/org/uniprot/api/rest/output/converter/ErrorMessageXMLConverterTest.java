package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class ErrorMessageXMLConverterTest {

    @Test
    void readReturnNull() {
        ErrorMessageXMLConverter converter = new ErrorMessageXMLConverter();
        ErrorInfo result = converter.read(null, null, null);
        assertNull(result);
    }

    @Test
    void readInternalReturnNull() {
        ErrorMessageXMLConverter converter = new ErrorMessageXMLConverter();
        ErrorInfo result = converter.readInternal(null, null);
        assertNull(result);
    }

    @Test
    void canWriteErrorMessage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        ErrorInfo errorInfo = new ErrorInfo("url", Collections.singletonList("message"));
        ErrorMessageXMLConverter converter = new ErrorMessageXMLConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("<errorInfo>"));
        assertTrue(result.contains("<messages>message</messages>"));
        assertTrue(result.contains("<url>url</url>"));
    }

    @Test
    void canWriteMultipleErrorMessages() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        List<String> messages = Arrays.asList("errorMessage1", "errorMessage2");
        ErrorInfo errorInfo = new ErrorInfo("url", messages);
        ErrorMessageXMLConverter converter = new ErrorMessageXMLConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("<errorInfo>"));
        assertTrue(result.contains("<messages>errorMessage1</messages>"));
        assertTrue(result.contains("<messages>errorMessage2</messages>"));
        assertTrue(result.contains("<url>url</url>"));
    }

    @Test
    void canWriteIfErrorInfoEntityAndXml() {
        assertTrue(
                new ErrorMessageXMLConverter()
                        .canWrite(null, ErrorInfo.class, MediaType.APPLICATION_XML));
    }

    @Test
    void cannotWriteIfErrorInfoEntityButNotXml() {
        assertFalse(
                new ErrorMessageXMLConverter()
                        .canWrite(null, ErrorInfo.class, MediaType.APPLICATION_JSON));
    }

    @Test
    void cannotWriteIfNotErrorInfoEntityAndIsXml() {
        assertFalse(
                new ErrorMessageXMLConverter()
                        .canWrite(null, String.class, MediaType.APPLICATION_XML));
    }

    private HttpOutputMessage getHttpOutputMessage(ByteArrayOutputStream outputStream) {
        return new HttpOutputMessage() {
            @Override
            public OutputStream getBody() {
                return outputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };
    }
}
