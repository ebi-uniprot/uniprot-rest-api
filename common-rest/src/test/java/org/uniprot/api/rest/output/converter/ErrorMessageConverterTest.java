package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * @author lgonzales
 */
class ErrorMessageConverterTest {

    @Test
    void canWriteErrorMessage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        ErrorInfo errorInfo = new ErrorInfo("url", Collections.singletonList("message"));
        ErrorMessageConverter converter = new ErrorMessageConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Error messages\nmessage", result);
    }

    @Test
    void canWriteMultipleErrorMessages() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        List<String> messages = Arrays.asList("errorMessage1", "errorMessage2");
        ErrorInfo errorInfo = new ErrorInfo("url", messages);
        ErrorMessageConverter converter = new ErrorMessageConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Error messages\nerrorMessage1\nerrorMessage2", result);
    }

    @Test
    void canWriteIfErrorInfoEntityAndFasta() {
        assertTrue(
                new ErrorMessageConverter()
                        .canWrite(null, ErrorInfo.class, UniProtMediaType.FASTA_MEDIA_TYPE));
    }

    @Test
    void cannotWriteIfErrorInfoEntityButInvalidFormat() {
        assertFalse(
                new ErrorMessageConverter()
                        .canWrite(null, ErrorInfo.class, MediaType.APPLICATION_JSON));
    }

    @Test
    void cannotWriteIfNotErrorInfoEntityAndFasta() {
        assertFalse(
                new ErrorMessageConverter()
                        .canWrite(null, String.class, UniProtMediaType.FASTA_MEDIA_TYPE));
    }

    @Test
    void readInternalThrowsException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new ErrorMessageConverter().readInternal(ErrorInfo.class, null));
    }

    @Test
    void readThrowsException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new ErrorMessageConverter().read(null, ErrorInfo.class, null));
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
