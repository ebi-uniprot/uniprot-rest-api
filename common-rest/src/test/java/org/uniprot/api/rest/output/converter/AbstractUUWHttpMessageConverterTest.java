package org.uniprot.api.rest.output.converter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created 26/09/18
 *
 * @author Edd
 */
public class AbstractUUWHttpMessageConverterTest {
    private static final String BEFORE = "before";
    private static final String AFTER = "after";
    private static final String ENTITY_SEPARATOR = ",";
    private static final MediaType ANY_MEDIA_TYPE = MediaType.ALL;
    private static final String ORIGINAL = "hello world";
    private static final String SEPARATED_ORIGINAL = ORIGINAL
            .codePoints()
            .mapToObj(c -> String.valueOf((char) c))
            .collect(Collectors.joining(ENTITY_SEPARATOR));
    private List<Character> listType;

    @Test
    public void overridenMethodsCalledInCorrectOrder_beforeThenWriteThenAfterThenCleanUp() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        converter.setOverrideAfter(true);
        converter.setOverrideBefore(true);
        assertThat(converter.hasCleanedUp(), is(false));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(os));

        assertThat(os.toString(), is(BEFORE + ORIGINAL + AFTER));
        assertThat(converter.hasCleanedUp(), is(true));
    }

    @Test
    public void canWriteMessageConvertersOnly() throws Exception {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);

        ParameterizedType type = (ParameterizedType) this.getClass().getDeclaredField("listType").getGenericType();
        assertThat(converter.canWrite(type, MessageConverterContext.class, ANY_MEDIA_TYPE), is(true));
        assertThat(converter.canWrite(type, String.class, ANY_MEDIA_TYPE), is(false));
    }

    @Test
    public void separatorUsedToSeparateEntities() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        converter.setEntitySeparator(ENTITY_SEPARATOR);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(os));

        assertThat(os.toString(), is(SEPARATED_ORIGINAL));
    }

    @Test
    public void errorDuringBeforeWritingCausesStreamToBeClosedAndCleanUp() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE){
            @Override
            protected void before(MessageConverterContext<Character> context, OutputStream outputStream) throws IOException {
                throw new IOException();
            }
        };
        assertThat(converter.hasCleanedUp(), is(false));
        assertThat(converter.isCharacterStreamIsClosed(), is(false));
        ByteArrayOutputStream mockOS = mock(ByteArrayOutputStream.class);
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(mockOS));

        verify(mockOS).close();
        assertThat(converter.hasCleanedUp(), is(true));
        assertThat(converter.isCharacterStreamIsClosed(), is(true));
    }

    @Test
    public void errorDuringAfterWritingCausesStreamToBeClosedAndCleanUp() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE){
            @Override
            protected void after(MessageConverterContext<Character> context, OutputStream outputStream) throws IOException {
                throw new IOException();
            }
        };
        assertThat(converter.hasCleanedUp(), is(false));
        assertThat(converter.isCharacterStreamIsClosed(), is(false));
        ByteArrayOutputStream mockOS = mock(ByteArrayOutputStream.class);
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(mockOS));

        verify(mockOS).close();
        assertThat(converter.hasCleanedUp(), is(true));
        assertThat(converter.isCharacterStreamIsClosed(), is(true));
    }

    @Test
    public void errorDuringWriteEntityCausesStreamToBeClosedAndCleanUp() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE){
            @Override
            protected void writeEntity(Character entity, OutputStream outputStream) throws IOException {
                throw new IOException();
            }
        };
        assertThat(converter.hasCleanedUp(), is(false));
        assertThat(converter.isCharacterStreamIsClosed(), is(false));
        ByteArrayOutputStream mockOS = mock(ByteArrayOutputStream.class);
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(mockOS));

        verify(mockOS).close();
        assertThat(converter.hasCleanedUp(), is(true));
        assertThat(converter.isCharacterStreamIsClosed(), is(true));
    }


    @Test
    public void writesCorrectNumberOfEntities() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        assertThat(converter.getCounter(), is(0));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(os));

        assertThat(converter.getCounter(), is(ORIGINAL.length()));
    }

    @Test
    public void gzipFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.GZIP);
        converter.writeInternal(context, null, httpOutputMessage(os));

        assertThat(os.toString(), is(zippedString(ORIGINAL)));
    }

    @Test
    public void normalFileTypeCreatesFileSuccessfully() throws IOException {
        FakeMessageConverter converter = new FakeMessageConverter(ANY_MEDIA_TYPE);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MessageConverterContext<Character> context = createFakeMessageConverterContext(FileType.FILE);
        converter.writeInternal(context, null, httpOutputMessage(os));

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
        private AtomicInteger counter = new AtomicInteger(0);
        private boolean hasCleanedUp;
        private boolean overrideBefore;
        private boolean overrideAfter;

        public boolean isCharacterStreamIsClosed() {
            return characterStreamIsClosed;
        }

        private boolean characterStreamIsClosed;
        private Stream<Character> characterStream = charStream(ORIGINAL).onClose(() -> characterStreamIsClosed = true);

        FakeMessageConverter(MediaType mediaType) {
            super(mediaType, Character.class);
        }

        int getCounter() {
            return counter.get();
        }

        boolean hasCleanedUp() {
            return hasCleanedUp;
        }

        void setOverrideBefore(boolean overrideBefore) {
            this.overrideBefore = overrideBefore;
        }

        void setOverrideAfter(boolean overrideAfter) {
            this.overrideAfter = overrideAfter;
        }

        @Override
        protected Stream<Character> entitiesToWrite(MessageConverterContext<Character> context) {
            return characterStream;
        }

        @Override
        protected void writeEntity(Character entity, OutputStream outputStream) throws IOException {
            outputStream.write(entity);
        }

        @Override
        protected void writeContents(MessageConverterContext<Character> context, OutputStream outputStream, Instant start, AtomicInteger counter) throws IOException {
            super.writeContents(context, outputStream, start, this.counter);
        }

        @Override
        protected void before(MessageConverterContext<Character> context, OutputStream outputStream) throws IOException {
            if (overrideBefore) {
                outputStream.write(BEFORE.getBytes());
            }
        }

        @Override
        protected void after(MessageConverterContext<Character> context, OutputStream outputStream) throws IOException {
            if (overrideAfter) {
                outputStream.write(AFTER.getBytes());
            }
        }

        @Override
        protected void cleanUp() {
            hasCleanedUp = true;
        }
    }
}