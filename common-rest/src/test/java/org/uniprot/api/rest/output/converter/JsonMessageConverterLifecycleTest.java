package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonMessageConverterLifecycleTest {
    @ParameterizedTest
    @EnumSource(
            value = FileType.class,
            names = {"FILE", "GZIP"})
    void lazyResponseCompletesBeforeResourcesClose(FileType fileType) throws Exception {
        Gatekeeper gatekeeper = new Gatekeeper(1);
        assertTrue(gatekeeper.enter());
        TrackingOutput output = new TrackingOutput(false);
        AtomicInteger consumed = new AtomicInteger();
        AtomicBoolean sourceClosed = new AtomicBoolean();
        Stream<String> entities =
                Stream.of("one", "two", "three")
                        .peek(value -> consumed.incrementAndGet())
                        .onClose(
                                () -> {
                                    assertEquals(3, consumed.get());
                                    assertFalse(output.closed);
                                    sourceClosed.set(true);
                                });
        var context = context(fileType, entities);
        assertEquals(0, consumed.get());

        converter(gatekeeper).writeInternal(context, null, message(output));

        assertTrue(sourceClosed.get());
        assertEquals(1, output.closes);
        assertEquals(1, gatekeeper.getSpaceInside());
        assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
        assertJson(output, fileType);
    }

    @ParameterizedTest
    @EnumSource(
            value = FileType.class,
            names = {"FILE", "GZIP"})
    void bufferedJsonClosesBeforeOutputAndPreservesOriginalFailure(FileType fileType)
            throws Exception {
        Gatekeeper gatekeeper = new Gatekeeper(1);
        assertTrue(gatekeeper.enter());
        RuntimeException original = new IllegalStateException("Failure before JSON footer flush");
        var converter =
                new JsonMessageConverter<String>(
                        new ObjectMapper(),
                        String.class,
                        mock(ReturnFieldConfig.class),
                        gatekeeper) {
                    @Override
                    protected void after(
                            MessageConverterContext<String> context, OutputStream output) {
                        throw original;
                    }
                };
        TrackingOutput output = new TrackingOutput(false);
        AtomicBoolean sourceClosed = new AtomicBoolean();
        var context =
                context(
                        fileType,
                        Stream.of("one", "two", "three").onClose(() -> sourceClosed.set(true)));

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () -> converter.writeInternal(context, null, message(output)));

        assertSame(original, thrown);
        assertEquals(0, thrown.getSuppressed().length);
        assertTrue(sourceClosed.get());
        assertEquals(1, output.closes);
        assertEquals(1, gatekeeper.getSpaceInside());
        assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
        assertJson(output, fileType);
    }

    @Test
    void disconnectedGzipOutputIsNotClosedTwiceAndSourceIsReleased() throws Exception {
        Gatekeeper gatekeeper = new Gatekeeper(1);
        assertTrue(gatekeeper.enter());
        TrackingOutput output = new TrackingOutput(true);
        AtomicBoolean sourceClosed = new AtomicBoolean();
        var context =
                context(
                        FileType.GZIP,
                        Stream.of("one", "two", "three").onClose(() -> sourceClosed.set(true)));

        IOException thrown =
                assertThrows(
                        IOException.class,
                        () -> converter(gatekeeper).writeInternal(context, null, message(output)));

        assertSame(output.closeFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
        assertTrue(sourceClosed.get());
        assertEquals(1, output.closes);
        assertEquals(1, gatekeeper.getSpaceInside());
        assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
    }

    @Test
    void sourceCloseFailureStillFinalizesJsonAndClosesOutput() throws Exception {
        Gatekeeper gatekeeper = new Gatekeeper(1);
        assertTrue(gatekeeper.enter());
        TrackingOutput output = new TrackingOutput(false);
        RuntimeException original = new IllegalStateException("Solr stream close failed");
        var context =
                context(
                        FileType.GZIP,
                        Stream.of("one", "two", "three")
                                .onClose(
                                        () -> {
                                            throw original;
                                        }));

        assertSame(
                original,
                assertThrows(
                        RuntimeException.class,
                        () -> converter(gatekeeper).writeInternal(context, null, message(output))));
        assertEquals(1, output.closes);
        assertEquals(1, gatekeeper.getSpaceInside());
        assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
        assertJson(output, FileType.GZIP);
    }

    @Test
    void failedJsonInitializationDoesNotMaskOriginalException() throws Exception {
        Gatekeeper gatekeeper = new Gatekeeper(1);
        assertTrue(gatekeeper.enter());
        RuntimeException original = new IllegalStateException("Cannot initialize JSON generator");
        var converter =
                new JsonMessageConverter<String>(
                        new ObjectMapper(),
                        String.class,
                        mock(ReturnFieldConfig.class),
                        gatekeeper) {
                    @Override
                    protected void before(
                            MessageConverterContext<String> context, OutputStream output) {
                        throw original;
                    }
                };
        TrackingOutput output = new TrackingOutput(false);
        AtomicBoolean sourceClosed = new AtomicBoolean();
        var context =
                context(
                        FileType.GZIP,
                        Stream.<String>empty().onClose(() -> sourceClosed.set(true)));

        assertSame(
                original,
                assertThrows(
                        RuntimeException.class,
                        () -> converter.writeInternal(context, null, message(output))));
        assertTrue(sourceClosed.get());
        assertEquals(1, output.closes);
        assertEquals(1, gatekeeper.getSpaceInside());
        assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
    }

    @Test
    void cleanupRemovesGeneratorWhenItsCloseThrowsRuntimeException() throws Exception {
        JsonGenerator generator = mock(JsonGenerator.class);
        RuntimeException failure = new IllegalStateException("Generator close failed");
        doThrow(failure).when(generator).close();
        JsonMessageConverter.TL_JSON_GENERATOR.set(generator);
        try {
            assertSame(
                    failure, assertThrows(RuntimeException.class, () -> converter(null).cleanUp()));
            assertNull(JsonMessageConverter.TL_JSON_GENERATOR.get());
        } finally {
            JsonMessageConverter.TL_JSON_GENERATOR.remove();
        }
    }

    private JsonMessageConverter<String> converter(Gatekeeper gatekeeper) {
        return new JsonMessageConverter<>(
                new ObjectMapper(), String.class, mock(ReturnFieldConfig.class), gatekeeper);
    }

    private MessageConverterContext<String> context(FileType type, Stream<String> entities) {
        return MessageConverterContext.<String>builder()
                .fileType(type)
                .entities(entities)
                .isLargeDownload(true)
                .build();
    }

    private MockHttpOutputMessage message(OutputStream output) {
        return new MockHttpOutputMessage() {
            @Override
            public OutputStream getBody() {
                return output;
            }
        };
    }

    private void assertJson(TrackingOutput output, FileType type) throws IOException {
        byte[] bytes = output.toByteArray();
        if (type == FileType.GZIP) {
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = input.readAllBytes();
            }
        }
        assertEquals(
                new ObjectMapper().readTree("{\"results\":[\"one\",\"two\",\"three\"]}"),
                new ObjectMapper().readTree(bytes));
    }

    private static class TrackingOutput extends ByteArrayOutputStream {
        private final boolean failClose;
        private final IOException closeFailure = new IOException("Client disconnected");
        private boolean closed;
        private int closes;

        TrackingOutput(boolean failClose) {
            this.failClose = failClose;
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            if (closed) {
                throw new IllegalStateException("Write after output close");
            }
            super.write(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closes++;
            if (failClose) {
                throw closeFailure;
            }
            super.close();
        }
    }
}
