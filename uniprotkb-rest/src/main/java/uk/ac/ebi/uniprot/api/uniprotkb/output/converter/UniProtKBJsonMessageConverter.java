package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.http.MediaType;

import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.StopStreamException;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.service.filters.EntryFilters;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.json.parser.uniprot.UniprotJsonConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class UniProtKBJsonMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    private static final Logger LOGGER = getLogger(UniProtKBJsonMessageConverter.class);
    private final ObjectMapper objectMapper = UniprotJsonConfig.getInstance().getSimpleObjectMapper();
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();

    public UniProtKBJsonMessageConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void before(MessageConverterContext context, OutputStream outputStream) throws IOException {
        tlFilters.set(FieldsParser.parseForFilters(context.getFields()));

        JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if(!context.isEntityOnly()) {
            generator.writeStartObject();

            if (context.getFacets() != null) {
                generator.writeFieldName("facets");
                generator.writeStartArray();
                context.getFacets().forEach(facet -> writeObject(generator, facet));
                generator.writeEndArray();
            }

            generator.writeFieldName("results");
            generator.writeStartArray();
        }

        tlJsonGenerator.set(generator);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();
        generator.writeObject(filterEntryContent(entity));
    }

    @Override
    protected void after(MessageConverterContext context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();

        if(!context.isEntityOnly()) {
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.flush();
        generator.close();
    }

    private void writeObject(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write Facet JSON object", e);
        }
    }

    private UniProtEntry filterEntryContent(UniProtEntry uniProtEntry) {
        Map<String, List<String>> filters = tlFilters.get();
        if (filters != null && !filters.isEmpty()) {
            uniProtEntry = EntryFilters.filterEntry(uniProtEntry, filters);
        }
        return uniProtEntry;
    }
}
