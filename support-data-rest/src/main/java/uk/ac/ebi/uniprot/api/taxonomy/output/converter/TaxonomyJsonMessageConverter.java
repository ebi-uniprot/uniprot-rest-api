package uk.ac.ebi.uniprot.api.taxonomy.output.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.StopStreamException;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;

import java.io.IOException;
import java.io.OutputStream;

public class TaxonomyJsonMessageConverter extends AbstractEntityHttpMessageConverter<TaxonomyEntry> {
    private final ObjectMapper objectMapper = TaxonomyJsonConfig.getInstance().getDefaultSimpleObjectMapper();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();

    public TaxonomyJsonMessageConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void before(MessageConverterContext<TaxonomyEntry> context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if (!context.isEntityOnly()) {
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

    private void writeObject(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write Facet JSON object", e);
        }
    }

    @Override
    protected void writeEntity(TaxonomyEntry entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();
        generator.writeObject(entity);

    }

    @Override
    protected void after(MessageConverterContext<TaxonomyEntry> context, OutputStream outputStream) throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();

        if (!context.isEntityOnly()) {
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.flush();
        generator.close();
    }
}