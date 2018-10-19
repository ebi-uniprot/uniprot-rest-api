package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 11/10/18
 *
 * @author Edd
 */
public class JsonMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext> {
    private static final Logger LOGGER = getLogger(JsonMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;
    private final Function<UniProtEntry, UPEntry> entryConverter = new EntryConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonMessageConverter() {
        super(MediaType.APPLICATION_JSON);
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
        // fields requested
        Map<String, List<String>> filters = FieldsParser.parseForFilters(messageConfig.getRequestDTO().getFields());

        // entries
        Stream<Collection<UniProtEntry>> entriesStream = (Stream<Collection<UniProtEntry>>) messageConfig.getEntities();
        AtomicBoolean firstIteration = new AtomicBoolean(true);

        try {
            outputStream.write("[".getBytes());

            entriesStream.forEach(items -> {
                items.stream()
                        .map(entryConverter)
                        .map(upEntry -> {
                            if (filters != null && !filters.isEmpty()) {
                                EntryFilters.filterEntry(upEntry, filters);
                            }
                            return upEntry;
                        })
                        .forEach(entry -> {
                            try {
                                int currentCount = counter.getAndIncrement();
                                if (currentCount % FLUSH_INTERVAL == 0) {
                                    outputStream.flush();
                                }
                                if (currentCount % 10000 == 0) {
                                    logStats(currentCount, start);
                                }

                                if (firstIteration.get()) {
                                    firstIteration.set(false);
                                } else {
                                    outputStream.write(",".getBytes());
                                }
                                outputStream.write(objectMapper.writeValueAsBytes(entry));

                            } catch (Throwable e) {
                                throw new StopStreamException("Could not write entry: " + entry
                                        .getAccession(), e);
                            }
                        });
            });
            outputStream.write("]".getBytes());

            logStats(counter.get(), start);
        } catch (StopStreamException | IOException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            entriesStream.close();
        } finally {
            outputStream.close();
        }
    }
}
