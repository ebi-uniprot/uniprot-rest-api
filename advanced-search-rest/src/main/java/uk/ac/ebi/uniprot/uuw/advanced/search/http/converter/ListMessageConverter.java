package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 23/08/18
 *
 * @author Edd
 */
public class ListMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext> {
    private static final Logger LOGGER = getLogger(FlatFileMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;

    public ListMessageConverter() {
        super(UniProtMediaType.LIST_MEDIA_TYPE);
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
    protected void write(MessageConverterContext messageConfig,
                         OutputStream outputStream,
                         Instant start,
                         AtomicInteger counter) throws IOException {
        Stream<String> entities = null;//(Stream<String>) messageConfig.getEntities();

        try {
            entities.forEach(id -> {
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

            logStats(counter.get(), start);
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            entities.close();
        } finally {
            outputStream.close();
        }
    }
}

