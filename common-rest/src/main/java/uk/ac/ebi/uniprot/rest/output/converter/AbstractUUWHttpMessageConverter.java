package uk.ac.ebi.uniprot.rest.output.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Abstract HTTP message converter extending {@link AbstractHttpMessageConverter} that implements a generic
 * way of writing the entities obtained from a {@link MessageConverterContext}. The default implementation
 * of this class makes use of a number of methods, which can be overriden in concrete classes to give the desired
 * behaviour.
 * <p>
 * Typically, the {@link AbstractUUWHttpMessageConverter#writeEntity(Object, OutputStream)} method need only
 * be overriden.
 *
 * Created 10/09/18
 *
 * @author Edd
 */
public abstract class AbstractUUWHttpMessageConverter<C, T> extends AbstractHttpMessageConverter<MessageConverterContext<C>> {
    private static final Logger LOGGER = getLogger(AbstractUUWHttpMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;
    private static final int LOG_INTERVAL = 10000;
    private String entitySeparator;

    AbstractUUWHttpMessageConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return MessageConverterContext.class.isAssignableFrom(aClass);
    }

    @Override
    protected MessageConverterContext<C> readInternal(Class<? extends MessageConverterContext<C>> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(MessageConverterContext<C> context, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();

        switch (context.getFileType()) {
            case GZIP:
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                    writeContents(context, gzipOutputStream, start, counter);
                }
                break;
            default:
                writeContents(context, outputStream, start, counter);
        }
    }

    protected void before(MessageConverterContext<C> context, OutputStream outputStream) throws IOException {
    }

    protected void after(MessageConverterContext<C> context, OutputStream outputStream) throws IOException {
    }

    protected abstract Stream<T> entitiesToWrite(MessageConverterContext<C> context);

    protected void writeContents(MessageConverterContext<C> context, OutputStream outputStream, Instant start, AtomicInteger counter) throws IOException {
        Stream<T> entities = entitiesToWrite(context);

        try {
            before(context, outputStream);

            writeEntities(entities, outputStream, start, counter);

            after(context, outputStream);
            logStats(counter.get(), start);
        } catch (StopStreamException | IOException e) {
            LOGGER.error("Error encountered when streaming data: closing stream.", e);
            entities.close();
        } finally {
            outputStream.close();
            cleanUp();
        }
    }

    protected void cleanUp() {
    }

    protected void setEntitySeparator(String separator) {
        this.entitySeparator = separator;
    }

    protected abstract void writeEntity(T entity, OutputStream outputStream) throws IOException;

    private void writeEntities(Stream<T> entityCollection, OutputStream outputStream, Instant start, AtomicInteger counter) {
        AtomicBoolean firstIteration = new AtomicBoolean(true);
        entityCollection.forEach(entity -> {
            try {
                int currentCount = counter.getAndIncrement();
                flushWhenNecessary(outputStream, currentCount);
                logWhenNecessary(start, currentCount);

                if (Objects.nonNull(entitySeparator)) {
                    if (firstIteration.get()) {
                        firstIteration.set(false);
                    } else {
                        outputStream.write(entitySeparator.getBytes());
                    }
                }

                writeEntity(entity, outputStream);
            } catch (Throwable e) {
                throw new StopStreamException("Could not write entry: " + entity, e);
            }
        });
    }

    private void logWhenNecessary(Instant start, int currentCount) {
        if (currentCount % LOG_INTERVAL == 0) {
            logStats(currentCount, start);
        }
    }

    private void flushWhenNecessary(OutputStream outputStream, int currentCount) throws IOException {
        if (currentCount % FLUSH_INTERVAL == 0) {
            outputStream.flush();
        }
    }

    private void logStats(int counter, Instant start) {
        Instant now = Instant.now();
        long millisDuration = Duration.between(start, now).toMillis();
        int secDuration = (int) millisDuration / 1000;
        String rate = String.format("%.2f", ((double) counter) / secDuration);
        LOGGER.info("Entities written: {}", counter);
        LOGGER.info("Entities duration: {} ({} entries/sec)", secDuration, rate);
    }
}
