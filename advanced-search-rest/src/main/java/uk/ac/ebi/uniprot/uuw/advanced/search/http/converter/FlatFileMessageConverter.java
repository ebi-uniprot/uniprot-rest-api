package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.kraken.ffwriter.line.impl.UniProtFlatfileWriter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class FlatFileMessageConverter extends AbstractHttpMessageConverter<MessageConverterContext> {
    public static final MediaType FF_MEDIA_TYPE = new MediaType("text", "flatfile");
    private static final Logger LOGGER = getLogger(FlatFileMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;

    public FlatFileMessageConverter() {
        super(FF_MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return MessageConverterContext.class.isAssignableFrom(aClass);
    }

    @Override
    protected MessageConverterContext readInternal(Class<? extends MessageConverterContext> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(MessageConverterContext contentStream, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();
        Instant start = Instant.now();
        Stream<Collection<UniProtEntry>> entities = (Stream<Collection<UniProtEntry>>)contentStream.getEntities();

        try {
            entities.forEach(items -> {
                items.forEach(entry -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount % FLUSH_INTERVAL == 0) {
                            outputStream.flush();
                        }
                        if (currentCount % 10000 == 0) {
                            logStats(currentCount, start);
                        }

                        outputStream.write((UniProtFlatfileWriter.write(entry) + "\n").getBytes());
                    } catch (IOException e) {
                        throw new StopStreamException("Could not write entry: " + entry, e);
                    }
                });
            });

            logStats(counter.get(), start);
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            entities.close();
        } finally {
            outputStream.flush();
        }
    }

    private void logStats(int counter, Instant start) {
        Instant now = Instant.now();
        long millisDuration = Duration.between(start, now).toMillis();
        int secDuration = (int) millisDuration / 1000;
        String rate = String.format("%.2f", ((double) counter) / secDuration);
        LOGGER.info("UniProt flatfile entries written: {}", counter);
        LOGGER.info("UniProt flatfile entries duration: {} ({} entries/sec)", secDuration, rate);
    }
}
