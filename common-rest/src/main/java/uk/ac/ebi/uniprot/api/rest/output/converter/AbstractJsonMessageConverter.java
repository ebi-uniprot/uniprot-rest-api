package uk.ac.ebi.uniprot.api.rest.output.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @param <T> instance of the object that is being written.
 *
 * @author lgonzales
 */
public abstract  class AbstractJsonMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private final ObjectMapper objectMapper;
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();

    public AbstractJsonMessageConverter(ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_JSON);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void before(MessageConverterContext context, OutputStream outputStream) throws IOException {
        tlFilters.set(getFilterFieldMap(context.getFields()));

        JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if(!context.isEntityOnly()) {
            generator.writeStartObject();

            if (context.getFacets() != null) {
                generator.writeFieldName("facets");
                generator.writeStartArray();
                for(Object facet: context.getFacets()){
                    writeFacet(generator,facet);
                }
                generator.writeEndArray();
            }

            generator.writeFieldName("results");
            generator.writeStartArray();
        }

        tlJsonGenerator.set(generator);
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
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

    protected Map<String, List<String>> getThreadLocalFilterMap(){
        return tlFilters.get();
    }

    protected abstract T filterEntryContent(T uniProtEntry);

    protected abstract Map<String, List<String>> getFilterFieldMap(String fields);

    private void writeFacet(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write Facet JSON object", e);
        }
    }

}
