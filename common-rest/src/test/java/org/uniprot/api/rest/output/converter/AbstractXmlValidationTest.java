package org.uniprot.api.rest.output.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created 29/11/2021
 *
 * @author Edd
 */
@Slf4j
public abstract class AbstractXmlValidationTest<T> {
    @TempDir File tempDir;

    public abstract String getXSDUrlLocation();

    public abstract MessageConverterContext<T> getMessageConverterConfig();

    public abstract AbstractEntityHttpMessageConverter<T> getXmlConverter();

    /**
     * This should be a comprehensive entry with all features possible, to ensure the validation
     * captures any potential problems.
     *
     * @return the entry to validate
     */
    protected abstract T getEntry();

    @BeforeEach
    void fetchXSD() throws IOException {
        FileUtils.copyURLToFile(new URL(getXSDUrlLocation()), getXSDFile());
    }

    @Test
    void validateEntry() throws IOException {
        AbstractEntityHttpMessageConverter<T> converter = getXmlConverter();

        HttpOutputMessage mockOutputMessage = mock(HttpOutputMessage.class);
        when(mockOutputMessage.getHeaders()).thenReturn(new HttpHeaders());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockOutputMessage.getBody()).thenReturn(outputStream);

        MessageConverterContext<T> context = getMessageConverterConfig();
        context.setEntities(Stream.of(getEntry()));

        converter.write(context, MediaType.APPLICATION_XML, mockOutputMessage);

        log.info(outputStream.toString());
        MatcherAssert.assertThat(
                "Schema validation failure", validateXMLSchema(outputStream), Matchers.is(true));
    }

    private boolean validateXMLSchema(ByteArrayOutputStream outputStream) {
        final List<SAXParseException> exceptions = new ArrayList<>();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(getXSDFile());
            Validator validator = schema.newValidator();
            validator.setErrorHandler(
                    new ErrorHandler() {
                        @Override
                        public void warning(SAXParseException exception) {
                            exceptions.add(exception);
                        }

                        @Override
                        public void fatalError(SAXParseException exception) {
                            exceptions.add(exception);
                        }

                        @Override
                        public void error(SAXParseException exception) {
                            exceptions.add(exception);
                        }
                    });

            validator.validate(
                    new StreamSource(new ByteArrayInputStream(outputStream.toByteArray())));
        } catch (IOException | SAXException e) {
            log.warn("Exception caught will be listed below");
        }

        if (!exceptions.isEmpty()) {
            exceptions.forEach(ex -> log.error("{} ----------\n", ex.getMessage(), ex));
            return false;
        } else {
            return true;
        }
    }

    private File getXSDFile() {
        return new File(tempDir.getAbsolutePath() + "/entry.xsd");
    }
}
