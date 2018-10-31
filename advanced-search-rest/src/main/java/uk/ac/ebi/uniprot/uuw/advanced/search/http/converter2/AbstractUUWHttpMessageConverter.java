package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public abstract class AbstractUUWHttpMessageConverter<C extends MessageConverterContext, T> extends AbstractHttpMessageConverter<C> {
    private static final Logger LOGGER = getLogger(AbstractUUWHttpMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;
    private static final int LOG_INTERVAL = 10000;
    private String entitySeparator;

    public AbstractUUWHttpMessageConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return MessageConverterContext.class.isAssignableFrom(aClass);
    }

    @Override
    protected C readInternal(Class<? extends C> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(C context, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();

        switch (context.getFileType()) {
            case GZIP:
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                    writeContents(context, counter, start, gzipOutputStream);
                }
                break;
            default:
                writeContents(context, counter, start, outputStream);
        }

        logStats(counter.get(), start);
    }

    private void writeContents(C context, AtomicInteger counter, Instant start, OutputStream outputStream) throws IOException {
        init(context);

        writeContents(context, outputStream, start, counter);
    }

    protected void init(C context) {
    }

    protected void before(C context, OutputStream outputStream) throws IOException {
    }

    protected void after(C context, OutputStream outputStream) throws IOException {
    }

//    protected abstract Supplier<T> entitySupplier

    @SuppressWarnings("unchecked")
    protected void writeContents(C context, OutputStream outputStream, Instant start, AtomicInteger counter) throws IOException {
        Stream<Collection<?>> entities = context.getEntities();

        try {
            before(context, outputStream);

            entities.forEach(
                    entityCollection -> writeCollection((Collection<T>) entityCollection, outputStream, start, counter));

            after(context, outputStream);
            logStats(counter.get(), start);
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
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

    private void writeCollection(Collection<T> entityCollection, OutputStream outputStream, Instant start, AtomicInteger counter) {
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


    protected abstract void writeEntity(T entity, OutputStream outputStream) throws IOException;

    void logStats(int counter, Instant start) {
        Instant now = Instant.now();
        long millisDuration = Duration.between(start, now).toMillis();
        int secDuration = (int) millisDuration / 1000;
        String rate = String.format("%.2f", ((double) counter) / secDuration);
        LOGGER.info("Entities written: {}", counter);
        LOGGER.info("Entities duration: {} ({} entries/sec)", secDuration, rate);
    }
}
