package uk.ac.ebi.uniprot.rest.output.converter;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.rest.output.context.FileType;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created 26/09/18
 *
 * @author Edd
 */
public class AbstractUUWHttpMessageConverterTest {
    private static final MediaType ANY_MEDIA_TYPE = MediaType.ALL;
    private static final String ORIGINAL = "hello world";

    @Test
    public void overridenMethodsCalledInCorrectOrder_beforeThenWriteThenAfterThenCleanUp() {
        
    }

    @Test
    public void supportsMessageConvertersOnly() {

    }

    @Test
    public void writesCorrectNumberOfEntities() {

    }

    @Test
    public void errorDuringWritingCausesWritingToStopAndStreamToBeClosed() {

    }

    @Test
    public void separatorUsedToSeparateEntities() {

    }

    @Test
    public void gzipFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.GZIP);
        converter.writeInternal(context, httpOutputMessage(os));

        assertThat(os.toString(), is(zippedString(ORIGINAL)));
    }

    @Test
    public void normalFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, httpOutputMessage(os));

        assertThat(os.toString(), is(ORIGINAL));
    }

    private static Stream<Character> charStream(String value) {
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

    private MessageConverterContext<Character> createFakeMessageConverterContext(FileType fileType) {
        return MessageConverterContext.<Character>builder()
                .fileType(fileType)
                .build();
    }

    private static class FakeMessageConverter extends AbstractUUWHttpMessageConverter<Character, Character> {
        FakeMessageConverter(MediaType mediaType) {
            super(mediaType);
        }

        @Override
        protected Stream<Character> entitiesToWrite(MessageConverterContext<Character> context) {
            return charStream(ORIGINAL);
        }

        @Override
        protected void writeEntity(Character entity, OutputStream outputStream) throws IOException {
            outputStream.write(entity);
        }
    }
}