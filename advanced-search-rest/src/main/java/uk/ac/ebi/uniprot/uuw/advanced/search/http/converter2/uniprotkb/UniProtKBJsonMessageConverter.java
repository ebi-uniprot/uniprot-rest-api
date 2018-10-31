package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.uniprotkb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.AbstractUUWHttpMessageConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UniProtKBJsonMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext, UniProtEntry> {
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Function<UniProtEntry, UPEntry> entryConverter = new EntryConverter();

    public UniProtKBJsonMessageConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void init(MessageConverterContext context) {
        tlFilters.set(FieldsParser.parseForFilters(context.getFields()));
        setEntitySeparator(",");
    }

    @Override
    protected void before(MessageConverterContext context, OutputStream outputStream) throws IOException {
        outputStream.write("{\"results\" : [".getBytes());
    }

    @Override
    protected void after(MessageConverterContext context, OutputStream outputStream) throws IOException {
        outputStream.write("]}".getBytes());
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write(objectMapper.writeValueAsBytes(uniProtEntry2UPEntry(entity)));
    }

    private UPEntry uniProtEntry2UPEntry(UniProtEntry uniProtEntry) {
        UPEntry upEntry = entryConverter.apply(uniProtEntry);
        Map<String, List<String>> filters = tlFilters.get();
        if (filters != null && !filters.isEmpty()) {
            EntryFilters.filterEntry(upEntry, filters);
        }
        return upEntry;
    }
}
