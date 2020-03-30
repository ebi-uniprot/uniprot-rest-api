package org.uniprot.api.rest.output.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.ReturnField;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param <T> instance of the object that is being written.
 * @author lgonzales
 */
public class JsonMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private static final String COMMA = "\\s*,\\s*";
    protected final ObjectMapper objectMapper;
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();
    private List<ReturnField> allFields;
    private JsonResponseFieldProjector fieldProjector;

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass,
            List<ReturnField> allFields) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.objectMapper = objectMapper;
        this.allFields = allFields;
        this.fieldProjector = new JsonResponseFieldProjector();
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        tlFilters.set(getFilterFieldMap(context.getFields()));

        JsonGenerator generator =
                objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if (!context.isEntityOnly()) {
            generator.writeStartObject();

            if (context.getFacets() != null) {
                generator.writeFieldName("facets");
                generator.writeStartArray();
                for (Object facet : context.getFacets()) {
                    writeFacet(generator, facet);
                }
                generator.writeEndArray();
            }

            if (context.getMatchedFields() != null) {
                generator.writeFieldName("matchedFields");
                generator.writeStartArray();
                for (Object matchedField : context.getMatchedFields()) {
                    writeFacet(generator, matchedField);
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
        generator.writeObject(projectEntryFields(entity));
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();

        if (!context.isEntityOnly()) {
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.flush();
        generator.close();
    }

    protected Map<String, List<String>> getThreadLocalFilterMap() {
        return tlFilters.get();
    }

    // returns only the required fields asked by the client or all fields in T.ResultFields enum
    protected Map<String, Object> projectEntryFields(T entity) {

        Map<String, List<String>> filterFieldMap = getThreadLocalFilterMap();

        Map<String, Object> result =
                this.fieldProjector.project(entity, filterFieldMap, this.allFields);
        return result;
    }

    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        if (Utils.notNullNotEmpty(fields)) {
            Map<String, List<String>> filters = new HashMap<>();
            for (String field : fields.split(COMMA)) {
                filters.put(field, Collections.emptyList());
            }
            return filters;
        } else {
            return Collections.emptyMap();
        }
    }

    private void writeFacet(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write Facet JSON object", e);
        }
    }
}
