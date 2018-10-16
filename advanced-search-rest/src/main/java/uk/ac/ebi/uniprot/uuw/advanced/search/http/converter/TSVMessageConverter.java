package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.download.DownloadableEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;

/**
 * Created 11/10/18
 *
 * @author Edd
 */
public class TSVMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext> {
    public static final String TSV_MEDIA_TYPE_VALUE = "text/tsv";
    public static final MediaType TSV_MEDIA_TYPE = new MediaType("text", "tsv");
    private static final Logger LOGGER = getLogger(TSVMessageConverter.class);
    private static final int FLUSH_INTERVAL = 5000;
    private final Function<UniProtEntry, UPEntry> entryConverter =new EntryConverter();
    public TSVMessageConverter() {
        super(TSV_MEDIA_TYPE);
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
        List<String> fields =  FieldsParser.parse(messageConfig.getRequestDTO().getFields());
        // entries
        Stream<Collection<UniProtEntry>> entriesStream = (Stream<Collection<UniProtEntry>>)messageConfig.getEntities();
        outputStream.write(convertHeader(fields).stream().collect(Collectors.joining("\t","", "\n")).getBytes());
        try {
            entriesStream.forEach(items -> {
                items.forEach(entry -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount % FLUSH_INTERVAL == 0) {
                            outputStream.flush();
                        }
                        if (currentCount % 10000 == 0) {
                            logStats(currentCount, start);
                        }
                        List<String> result = convert(entry, filters, fields);
                        outputStream.write(result.stream().collect(Collectors.joining("\t","", "\n")).getBytes());
                    } catch (Throwable e) {
                        throw new StopStreamException("Could not write entry: " + entry.getPrimaryUniProtAccession().getValue(), e);
                    }
                });
            });

            logStats(counter.get(), start);
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            entriesStream.close();
        } finally {
            outputStream.close();
        }
    }
    private List<String> convertHeader(List<String> fields){
    	return fields.stream().map(this::getFieldDisplayName)
    	.collect(Collectors.toList());
    }
    private String getFieldDisplayName(String field) {
    	Optional<Field> opField =UniProtResultFields.INSTANCE.getField(field);
    	if(opField.isPresent())
    		return opField.get().getLabel();
    	else
    		return field;
    }
    private List<String> convert(UniProtEntry upEntry, Map<String, List<String>> filterParams, List<String> fields ) {
    	 UPEntry entry = entryConverter.apply(upEntry);
         if ((filterParams != null) && !filterParams.isEmpty())
        	 EntryFilters.filterEntry(entry, filterParams);
         DownloadableEntry dlEntry = new DownloadableEntry(entry, fields);
         return dlEntry.getData();
    }
}
