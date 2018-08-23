package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 23/08/18
 *
 * @author Edd
 */
public class ListMessageConverter extends AbstractHttpMessageConverter<Stream<String>> {
    public static final MediaType MEDIA_TYPE = new MediaType("text", "list");
    private static final Logger LOGGER = getLogger(FlatFileMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;

    public ListMessageConverter() {
        super(MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return Stream.class.isAssignableFrom(aClass);
    }

    @Override
    protected Stream<String> readInternal(Class<? extends Stream<String>> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(Stream<String> contentStream, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();

        try {
            contentStream.forEach(id -> {
                try {
                    int currentCount = counter.getAndIncrement();
                    if (currentCount % FLUSH_INTERVAL == 0) {
                        outputStream.flush();
                    }
                    outputStream.write((id + "\n").getBytes());
                } catch (IOException e) {
                    throw new StopStreamException("Could not write id: " + id, e);
                }

            });

            Instant now = Instant.now();
            long millisDuration = Duration.between(start, now).toMillis();
            int secDuration = (int) millisDuration / 1000;
            int totalCount = counter.get();
            String rate = String.format("%.2f", ((double) totalCount) / secDuration);
            LOGGER.info("IDs written: {}", totalCount);
            LOGGER.info("IDs writing duration: {} ({} entries/sec)", secDuration, rate);
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            contentStream.close();
        } finally {
            outputStream.flush();
        }
    }
}

