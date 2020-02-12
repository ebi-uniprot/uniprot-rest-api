package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.bohnman.squiggly.context.provider.SimpleSquigglyContextProvider;
import com.github.bohnman.squiggly.filter.SquigglyPropertyFilter;
import com.github.bohnman.squiggly.parser.SquigglyParser;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.ReturnField;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @param <T> instance of the object that is being written.
 * @author lgonzales
 */
public class JsonMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private static final String EXCLUDE_FILTER_ID = "dynamicExclude";
    private static final String COMMA = "\\s*,\\s*";
    private final ObjectMapper objectMapper;
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.objectMapper = objectMapper;
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
        ObjectWriter objectWriter = objectMapper.writer();
        Set<String> fieldFilters = getFilters();
        if(Utils.notNullOrEmpty(fieldFilters)) {
            FilterProvider filters = configFilters(fieldFilters);
            objectWriter = objectWriter.with(filters);
        }
        objectWriter.writeValue(generator, entity);
    }

    private Set<String> getFilters() {
        Set<String> fieldFilters = getThreadLocalFilterMap().values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        fieldFilters.addAll(getThreadLocalFilterMap().keySet());
        return fieldFilters;
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

    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        if (Utils.notNullOrEmpty(fields)) {
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

    private FilterProvider configFilters(Set<String> fields) {
        SimpleFilterProvider filterProvider = null;
        if(Utils.notNullOrEmpty(fields)) {
            String fieldRequest = String.join(",", fields);
            SquigglyPropertyFilter filter =
                    new SquigglyPropertyFilter(
                            new SimpleSquigglyContextProvider(new SquigglyParser(), fieldRequest));

            filterProvider = new SimpleFilterProvider()
                    .addFilter(SquigglyPropertyFilter.FILTER_ID, filter);
        }
        return filterProvider;
    }

}
