package org.uniprot.api.rest.output.converter;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
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
    protected final ObjectMapper objectMapper;

    private ThreadLocal<List<ReturnField>> tlFilters = new ThreadLocal<>();
    protected ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();
    private ThreadLocal<ObjectMapper> tlFilterMapper = new ThreadLocal<>();

    private SquigglyParser squigglyParser = new SquigglyParser();
    private Pattern filterPattern = Pattern.compile("\\((.*?)\\)");
    private ReturnFieldConfig returnFieldConfig;

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass,
            ReturnFieldConfig returnFieldConfig) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.objectMapper = objectMapper;
        this.returnFieldConfig = returnFieldConfig;
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        List<ReturnField> fieldList = getFilterFieldMap(context.getFields());
        tlFilters.set(fieldList);
        if (notNullNotEmpty(context.getFields())) {
            ObjectMapper filterMapper = objectMapper.copy();
            FilterProvider filterProvider = getFieldsFilterProvider(fieldList);
            filterMapper.setFilterProvider(filterProvider);
            tlFilterMapper.set(filterMapper);
        }

        JsonGenerator generator =
                objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if (!context.isEntityOnly()) {
            generator.writeStartObject();

            if (notNullNotEmpty(context.getFacets())) {
                generator.writeFieldName("facets");
                generator.writeStartArray();
                for (Object facet : context.getFacets()) {
                    writeFacet(generator, facet);
                }
                generator.writeEndArray();
            }

            if (notNullNotEmpty(context.getMatchedFields())) {
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
        List<ReturnField> fields = tlFilters.get();
        if (Utils.notNullNotEmpty(fields)) {
            ObjectMapper mapper = tlFilterMapper.get();
            String filteredJson = mapper.writeValueAsString(entity);

            Map<String, String> filterFields = getFieldsWithFilterMap(fields);
            if (notNullNotEmpty(filterFields)) {
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

    protected List<ReturnField> getFilterFieldMap(String fields) {
        if (notNullNotEmpty(fields)) {
            List<ReturnField> filters = new ArrayList<>();
            for (String field : fields.split(COMMA)) {
                filters.add(returnFieldConfig.getReturnFieldByName(field));
            }
            filters.addAll(returnFieldConfig.getRequiredReturnFields());
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
                        .map(this::getPathWithoutFilter)
                        .map(path -> path.replaceAll("\\[\\*]", ""))
                        .map(path -> path += ".*")
                        .collect(Collectors.joining(","));

        SquigglyPropertyFilter filter =
                new SquigglyPropertyFilter(
                        new SimpleSquigglyContextProvider(squigglyParser, fieldsPath));

        return new SimpleFilterProvider().addFilter(SquigglyPropertyFilter.FILTER_ID, filter);
    }

    private String getPathWithoutFilter(String path) {
        if (path.contains("[?")) {
            return path.substring(0, path.indexOf("[?"));
        } else {
            return path;
        }
    }

    private Map<String, String> getFieldsWithFilterMap(List<ReturnField> fields) {
        Map<String, String> filters = new HashMap<>();
        fields.stream()
                .flatMap(returnField -> returnField.getPaths().stream())
                .filter(path -> path.contains("[?"))
                .forEach(fullPath -> addFilterPath(filters, fullPath));
        return filters;
    }

    private void addFilterPath(Map<String, String> filters, String fullPath) {
        String path = fullPath.substring(0, fullPath.indexOf("[?"));
        String filter = fullPath.substring(fullPath.indexOf("[?"));
        if (filters.containsKey(path)) {
            String currentFilter = filters.get(path);
            Matcher match = filterPattern.matcher(currentFilter);
            Matcher newMatch = filterPattern.matcher(filter);
            if (match.find() && newMatch.find()) {
                String newFilter = "[?(" + match.group(1) + " || " + newMatch.group(1) + ")]";
                filters.put(path, newFilter);
            }
        } else {
            filters.put(path, filter);
        }
    }
}
