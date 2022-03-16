package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;

/**
 * Abstract HTTP message converter extending {@link AbstractHttpMessageConverter} that implements a
 * generic way of writing the entities obtained from a {@link MessageConverterContext}. The default
 * implementation of this class makes use of a number of methods, which can be overriden in concrete
 * classes to give the desired behaviour.
 *
 * <p>Typically, the {@link AbstractUUWHttpMessageConverter#writeEntity(Object, OutputStream)}
 * method need only be overriden.
 *
 * <p>Created 10/09/18
 *
 * @author Edd
 */
@Slf4j
public abstract class AbstractUUWHttpMessageConverter<C, T>
        extends AbstractGenericHttpMessageConverter<MessageConverterContext<C>> {
    private static final int FLUSH_INTERVAL = 5000;
    private static final int LOG_INTERVAL = 10000;
    private static final ThreadLocal<String> ENTITY_SEPARATOR = new ThreadLocal<>();
    private final Class<C> messageConverterEntryClass;
    private final Gatekeeper downloadGatekeeper;

    AbstractUUWHttpMessageConverter(
            MediaType mediaType,
            Class<C> messageConverterEntryClass,
            Gatekeeper downloadGatekeeper) {
        super(mediaType);
        this.messageConverterEntryClass = messageConverterEntryClass;
        this.downloadGatekeeper = downloadGatekeeper;
    }

    @Override
    public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
        boolean result = false;
        if (Objects.nonNull(type)
                && validMediaType(mediaType)
                && MessageConverterContext.class.isAssignableFrom(clazz)
                && Utils.notNull(type)) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            String typeClassName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            result = typeClassName.equals(messageConverterEntryClass.getName());
        }
        return result;
    }

    @Override
    public boolean canRead(
            Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public MessageConverterContext<C> read(
            Type type, Class<?> aClass, HttpInputMessage httpInputMessage) {
        return null;
    }

    protected boolean validMediaType(MediaType mediaType) {
        if (Utils.notNull(mediaType)) {
            for (MediaType supportedMediaType : getSupportedMediaTypes()) {
                if (mediaType.equals(supportedMediaType)) {
                    return true;
                }
            }
        }

        // the following (taken from
        // org.springframework.http.converter.AbstractHttpMessageConverter)
        // can be simplified but this way is more readable.
        if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
            return true;
        }

        return false;
    }

    @Override
    protected MessageConverterContext<C> readInternal(
            Class<? extends MessageConverterContext<C>> aClass, HttpInputMessage httpInputMessage) {
        return null;
    }

    @Override
    protected void writeInternal(
            MessageConverterContext<C> context, Type type, HttpOutputMessage httpOutputMessage)
            throws IOException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();

        if (context.getFileType() == FileType.GZIP) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                writeContents(context, gzipOutputStream, start, counter);
            }
        } else {
            writeContents(context, outputStream, start, counter);
        }
    }

    protected void before(MessageConverterContext<C> context, OutputStream outputStream)
            throws IOException {}

    protected void after(MessageConverterContext<C> context, OutputStream outputStream)
            throws IOException {}

    protected abstract Stream<T> entitiesToWrite(MessageConverterContext<C> context);

    protected void writeContents(
            MessageConverterContext<C> context,
            OutputStream outputStream,
            Instant start,
            AtomicInteger counter)
            throws IOException {
        Stream<T> entities = entitiesToWrite(context);

        try {
            before(context, outputStream);
            writeEntities(entities, outputStream, start, counter);
            after(context, outputStream);
            logStats(counter.get(), start);
        } catch (StopStreamException | IOException e) {
            String errorMsg = "Error encountered when streaming data.";
            outputStream.write(("\n\n" + errorMsg + " Please try again later.\n").getBytes());
            throw new StopStreamException(errorMsg, e);
        } finally {
            if (downloadGatekeeper != null && context.isLargeDownload()) {
                downloadGatekeeper.exit();
                log.debug(
                        "Gatekeeper let me out (space inside={})",
                        downloadGatekeeper.getSpaceInside());
            }

            outputStream.close();
            entities.close();
            cleanUp();
        }
    }

    protected void cleanUp() {
        ENTITY_SEPARATOR.remove();
    }

    protected void setEntitySeparator(String separator) {
        ENTITY_SEPARATOR.set(separator);
    }

    protected abstract void writeEntity(T entity, OutputStream outputStream) throws IOException;

    protected void writeEntities(
            Stream<T> entityCollection,
            OutputStream outputStream,
            Instant start,
            AtomicInteger counter) {
        AtomicBoolean firstIteration = new AtomicBoolean(true);
        try {

            entityCollection.forEach(
                    entity -> {
                        // TODO: 16/03/2022 if StoreResult entity is not null, do the following:
                        try {
                            int currentCount = counter.getAndIncrement();
                            flushWhenNecessary(outputStream, currentCount);
                            logWhenNecessary(start, currentCount);

                            if (Objects.nonNull(ENTITY_SEPARATOR.get())) {
                                if (firstIteration.get()) {
                                    firstIteration.set(false);
                                } else {
                                    writeEntitySeparator(outputStream, ENTITY_SEPARATOR.get());
                                }
                            }

                            writeEntity(entity, outputStream);
                        } catch (Exception e) {
                            throw new StopStreamException("Could not write entry: " + entity, e);
                        }
                        // TODO: 16/03/2022 else, add StoreResult.id to failed Ids
                    });
        } catch (Exception e) {
            // TODO: 16/03/2022 call new abstract method, onWritingStreamError(e, outputStream);
            throw new StopStreamException("Stream must be closed", e);
        }
    }

    // TODO: 16/03/2022
    // protected void onWritingStreamError(e, outputStream) { throw e;}
    // TODO: 16/03/2022
    // override this method for JsonMessageConverter, to close the results array, and write
    // an error object with message, "Error encountered when streaming data. Please try again later."
    protected void writeEntitySeparator(OutputStream outputStream, String separator)
            throws IOException {
        outputStream.write(separator.getBytes());
    }

    private void logWhenNecessary(Instant start, int currentCount) {
        if (currentCount > 0 && currentCount % LOG_INTERVAL == 0) {
            logStats(currentCount, start);
        }
    }

    private void flushWhenNecessary(OutputStream outputStream, int currentCount)
            throws IOException {
        if (currentCount % FLUSH_INTERVAL == 0) {
            outputStream.flush();
        }
    }

    private void logStats(int counter, Instant start) {
        Instant now = Instant.now();
        long millisDuration = Duration.between(start, now).toMillis();
        int secDuration = (int) millisDuration / 1000;
        String rate = String.format("%.2f", ((double) counter) / secDuration);
        log.info(
                "Entities written by {}: {} with duration: {} ({} entries/sec)",
                getClass(),
                counter,
                secDuration,
                rate);
    }
}
