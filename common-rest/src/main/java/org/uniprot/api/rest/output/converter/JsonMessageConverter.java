package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.returnfield.model.ReturnField;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.bohnman.squiggly.context.provider.SimpleSquigglyContextProvider;
import com.github.bohnman.squiggly.filter.SquigglyPropertyFilter;
import com.github.bohnman.squiggly.parser.SquigglyParser;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * @param <T> instance of the object that is being written.
 * @author lgonzales
 */
public class JsonMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private static final String COMMA = "\\s*,\\s*";
    private static final String PATH_PREFIX = "$..";
    private final ObjectMapper objectMapper;
    private ThreadLocal<List<ReturnField>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();
    private List<ReturnField> allFields;

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass,
            List<ReturnField> allFields) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.objectMapper = objectMapper;
        this.allFields = allFields;
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
        List<ReturnField> fields = getThreadLocalFilterMap();
        if (Utils.notNullNotEmpty(fields)) {
            ObjectMapper mapper = objectMapper.copy();

            FilterProvider filterProvider = getFieldsFilterProvider(fields);
            mapper.setFilterProvider(filterProvider);

            String filteredJson = mapper.writeValueAsString(entity);

            Map<String, String> filterFields = getFieldsWithFilterMap(fields);
            if (Utils.notNullNotEmpty(filterFields)) {
                DocumentContext referenceJson = JsonPath.parse(filteredJson);
                for (Map.Entry<String, String> field : filterFields.entrySet()) {
                    String path = PATH_PREFIX + field.getKey() + field.getValue();
                    JsonPath filterPath = JsonPath.compile(path);
                    Object filteredItems = referenceJson.read(filterPath);

                    JsonPath setPath = JsonPath.compile(PATH_PREFIX + field.getKey());
                    referenceJson = referenceJson.set(setPath, filteredItems);
                }
                filteredJson = referenceJson.jsonString();
            }
            generator.writeRaw(filteredJson);
        } else {
            generator.writeObject(entity);
        }
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

    protected List<ReturnField> getThreadLocalFilterMap() {
        return tlFilters.get();
    }

    protected List<ReturnField> getFilterFieldMap(String fields) {
        if (Utils.notNullNotEmpty(fields)) {
            List<ReturnField> filters = new ArrayList<>();
            for (String field : fields.split(COMMA)) {
                allFields.stream()
                        .filter(fieldItem -> fieldItem.getName().equals(field))
                        .findFirst()
                        .ifPresent(filters::add);
            }
            allFields.stream()
                    .filter(ReturnField::getIsRequired)
                    .forEach(filters::add); // add required fields
            return filters;
        } else {
            return Collections.emptyList();
        }
    }

    private void writeFacet(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write Facet JSON object", e);
        }
    }

    private SimpleFilterProvider getFieldsFilterProvider(List<ReturnField> fields) {
        String fieldsPath =
                fields.stream()
                        .flatMap(returnField -> returnField.getPaths().stream())
                        .map(
                                path -> {
                                    if (path.contains("[?")) {
                                        return path.substring(0, path.indexOf("[?"));
                                    } else {
                                        return path;
                                    }
                                })
                        .map(path -> path.replaceAll("\\[\\*]", ""))
                        .map(path -> path += ".*")
                        .collect(Collectors.joining(","));

        SquigglyPropertyFilter filter =
                new SquigglyPropertyFilter(
                        new SimpleSquigglyContextProvider(new SquigglyParser(), fieldsPath));

        return new SimpleFilterProvider().addFilter(SquigglyPropertyFilter.FILTER_ID, filter);
    }

    private Map<String, String> getFieldsWithFilterMap(List<ReturnField> fields) {
        Map<String, String> filters = new HashMap<>();
        fields.stream()
                .flatMap(returnField -> returnField.getPaths().stream())
                .filter(path -> path.contains("[?"))
                .forEach(
                        fullPath -> {
                            String path = fullPath.substring(0, fullPath.indexOf("[?"));
                            String filter = fullPath.substring(fullPath.indexOf("[?"));
                            if (filters.containsKey(path)) {
                                String currentFilter = filters.get(path);
                                Pattern p = Pattern.compile("\\((.*?)\\)");
                                Matcher existingMatch = p.matcher(currentFilter);
                                Matcher newMatch = p.matcher(filter);
                                if (existingMatch.find() && newMatch.find()) {
                                    String newFilter =
                                            "[?("
                                                    + existingMatch.group(1)
                                                    + " || "
                                                    + newMatch.group(1)
                                                    + ")]";
                                    filters.put(path, newFilter);
                                }
                            } else {
                                filters.put(path, filter);
                            }
                        });
        return filters;
    }
}
