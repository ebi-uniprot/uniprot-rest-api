package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public abstract class AbstractUUWHttpMessageConverter<T extends MessageConverterContext> extends AbstractHttpMessageConverter<T> {
    private static final Logger LOGGER = getLogger(AbstractUUWHttpMessageConverter.class);

    public AbstractUUWHttpMessageConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return false;
    }

    @Override
    protected T readInternal(Class<? extends T> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();

        switch (t.getFileType()) {
            case GZIP:
                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                    write(t, gzipOutputStream, start, counter);
                }
                break;
            default:
                 write(t, outputStream, start, counter);
        }

        logStats(counter.get(), start);
    }

    protected abstract void write(T t, OutputStream outputStream, Instant start, AtomicInteger counter) throws IOException;

    void logStats(int counter, Instant start) {
        Instant now = Instant.now();
        long millisDuration = Duration.between(start, now).toMillis();
        int secDuration = (int) millisDuration / 1000;
        String rate = String.format("%.2f", ((double) counter) / secDuration);
        LOGGER.info("Entities written: {}", counter);
        LOGGER.info("Entities duration: {} ({} entries/sec)", secDuration, rate);
    }
}
