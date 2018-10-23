package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.EntryGffConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.GFF_MEDIA_TYPE;

/**
 * 
 * @author gqi
 *
 */
public class GffMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext> {
    private static final Logger LOGGER = getLogger(GffMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;

    public GffMessageConverter() {
        super(GFF_MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return MessageConverterContext.class.isAssignableFrom(aClass);
    }

    @Override
    protected MessageConverterContext readInternal(Class<? extends MessageConverterContext> aClass,
            HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void write(MessageConverterContext messageConfig,
            OutputStream outputStream,
            Instant start,
            AtomicInteger counter) throws IOException {
        Stream<Collection<UniProtEntry>> entities = (Stream<Collection<UniProtEntry>>) messageConfig.getEntities();

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

                        outputStream.write((EntryGffConverter.convert(entry) + "\n").getBytes());
                    } catch (Throwable e) {
                        throw new StopStreamException("Could not write entry: " + entry, e);
                    }
                });
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
