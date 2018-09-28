package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.FileType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created 26/09/18
 *
 * @author Edd
 */
public class AbstractUUWHttpMessageConverterTest {
    private static final MediaType ANY_MEDIA_TYPE = MediaType.ALL;
    private static final String ORIGINAL = "hello world";

    @Test
    public void gzipFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext context = createFakeMessageConverterContext(FileType.GZIP);
        converter.writeInternal(context, httpOutputMessage(os));

        assertThat(os.toString(), is(zippedString(ORIGINAL)));
    }

    @Test
    public void normalFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, httpOutputMessage(os));

        assertThat(os.toString(), is(ORIGINAL));
    }

    private Stream<Character> charStream(String value) {
        return value.chars().mapToObj(c -> (char) c);
    }

    private String zippedString(String value) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(value.length());
        GZIPOutputStream gos = new GZIPOutputStream(byteStream);
        gos.write(value.getBytes());
        gos.close();
        return byteStream.toString();
    }

    private HttpOutputMessage httpOutputMessage(ByteArrayOutputStream os) {
        return new HttpOutputMessage() {
            @Override
            public OutputStream getBody() {
                return os;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };
    }

    private MessageConverterContext createFakeMessageConverterContext(FileType fileType) {
        MessageConverterContext context = new MessageConverterContext();
        context.setFileType(fileType);
        context.setEntities(charStream(ORIGINAL));
        return context;
    }

    private static class FakeMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext> {
        private static final Logger LOGGER = LoggerFactory.getLogger(FakeMessageConverter.class);
        FakeMessageConverter(MediaType mediaType) {
            super(mediaType);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void write(MessageConverterContext messageConverterContext, OutputStream outputStream, Instant start, AtomicInteger counter) throws IOException {
            Stream<Character> entities = (Stream<Character>) messageConverterContext.getEntities();
            entities.forEach(entity -> {
                try {
                    outputStream.write(entity);
                } catch (IOException e) {
                    LOGGER.error("Error during test", e);
                }
            });
            outputStream.close();
        }
    }
}