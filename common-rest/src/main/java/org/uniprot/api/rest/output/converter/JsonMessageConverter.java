package org.uniprot.api.rest.output.converter;

import static org.uniprot.core.util.Utils.notNull;
import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.ExtraOptions;
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
import com.jayway.jsonpath.PathNotFoundException;

/**
 * @param <T> instance of the object that is being written.
 * @author lgonzales
 */
public class JsonMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private static final String PATH_PREFIX = "$..";
    protected final ObjectMapper objectMapper;

    private static final ThreadLocal<List<ReturnField>> TL_FILTERS = new ThreadLocal<>();
    protected static final ThreadLocal<JsonGenerator> TL_JSON_GENERATOR = new ThreadLocal<>();
    private static final ThreadLocal<ObjectMapper> TL_FILTER_MAPPER = new ThreadLocal<>();

    private static final SquigglyParser SQUIGGLY_PARSER = new SquigglyParser();
    private static final Pattern FILTER_PATTERN = Pattern.compile("\\((.*?)\\)");
    private final ReturnFieldConfig returnFieldConfig;

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass,
            ReturnFieldConfig returnFieldConfig) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.objectMapper = objectMapper;
        this.returnFieldConfig = returnFieldConfig;
    }

    public JsonMessageConverter(
            ObjectMapper objectMapper,
            Class<T> messageConverterEntryClass,
            ReturnFieldConfig returnFieldConfig,
            Gatekeeper downloadGatekeeper) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass, downloadGatekeeper);
        this.objectMapper = objectMapper;
        this.returnFieldConfig = returnFieldConfig;
    }

    @Override
    @SuppressWarnings("squid:S2095")
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        List<ReturnField> fieldList = getFilterFieldMap(context.getFields());
        TL_FILTERS.set(fieldList);
        if (notNullNotEmpty(context.getFields())) {
            ObjectMapper filterMapper = objectMapper.copy();
            FilterProvider filterProvider = getFieldsFilterProvider(fieldList);
            filterMapper.setFilterProvider(filterProvider);
            TL_FILTER_MAPPER.set(filterMapper);
            setEntitySeparator(",\n");
        }

        JsonGenerator generator =
                objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

        if (!context.isEntityOnly()) {
            generator.writeStartObject();

            if (notNullNotEmpty(context.getWarnings())) {
                writeJsonArray(generator, "warnings", context.getWarnings());
            }

            if (notNullNotEmpty(context.getFacets())) {
                writeJsonArray(generator, "facets", context.getFacets());
            }

            if (notNullNotEmpty(context.getMatchedFields())) {
                writeJsonArray(generator, "matchedFields", context.getMatchedFields());
            }

            generator.writeFieldName("results");
            generator.writeStartArray();
        }

        TL_JSON_GENERATOR.set(generator);
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = TL_JSON_GENERATOR.get();
        List<ReturnField> fields = TL_FILTERS.get();
        if (Utils.notNullNotEmpty(fields)) {
            ObjectMapper mapper = TL_FILTER_MAPPER.get();
            String filteredJson = mapper.writeValueAsString(entity);

            Map<String, String> filterFields = getFieldsWithFilterMap(fields);
            if (notNullNotEmpty(filterFields)) {
                DocumentContext referenceJson = JsonPath.parse(filteredJson);
                for (Map.Entry<String, String> field : filterFields.entrySet()) {
                    String path = PATH_PREFIX + field.getKey() + field.getValue();
                    JsonPath filterPath = JsonPath.compile(path);
                    Object filteredItems = referenceJson.read(filterPath);

                    JsonPath setPath = JsonPath.compile(PATH_PREFIX + field.getKey());
                    try {
                        referenceJson = referenceJson.set(setPath, filteredItems);
                    } catch (PathNotFoundException pnfe) {
                        logger.debug("Unable to set path " + setPath.getPath());
                    }
                }
                filteredJson = referenceJson.jsonString();
            }
            generator.writeRaw(filteredJson);
        } else {
            generator.writeObject(entity);
        }
    }

    @Override
    protected void writeEntitySeparator(OutputStream outputStream, String entitySeparator)
            throws IOException {
        JsonGenerator generator = TL_JSON_GENERATOR.get();
        generator.writeRaw(entitySeparator);
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        JsonGenerator generator = TL_JSON_GENERATOR.get();

        if (!context.isEntityOnly()) {
            generator.writeEndArray();

            if (notNull(context.getExtraOptions())) {
                ExtraOptions extraOptions = context.getExtraOptions();
                if (notNullNotEmpty(extraOptions.getFailedIds())) {
                    writeJsonArray(generator, "failedIds", extraOptions.getFailedIds());
                }
                if (notNullNotEmpty(extraOptions.getSuggestedIds())) {
                    writeJsonArray(generator, "suggestedIds", extraOptions.getSuggestedIds());
                }

                if (Objects.nonNull(extraOptions.getObsoleteCount())) {
                    writeJsonObject(generator, "obsoleteCount", extraOptions.getObsoleteCount());
                }
            }

            if (notNullNotEmpty(context.getSuggestions())) {
                writeJsonArray(generator, "suggestions", context.getSuggestions());
            }

            generator.writeEndObject();
        }
        generator.flush();
    }

    @Override
    protected void writeStopStreamErrorMessage(OutputStream outputStream, String errorMsg)
            throws IOException {
        JsonGenerator generator = TL_JSON_GENERATOR.get();
        generator.writeEndArray();
        generator.writeFieldName("error");
        generator.writeObject(errorMsg);
        generator.writeEndObject();
        // do not move flush to cleanUp() method, because it won't be called
        // instead we're throwing another StopStreamException in AbstractUUWHttpMessageConverter,
        // which
        // is then handled in ResponseExceptionHandler
        generator.flush();
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        try {
            TL_JSON_GENERATOR.get().close();
        } catch (IOException e) {
            logger.warn("Unable to close json generator", e);
        }
        TL_JSON_GENERATOR.remove();
        TL_FILTER_MAPPER.remove();
        TL_FILTERS.remove();
    }

    protected List<ReturnField> getFilterFieldMap(String fields) {
        if (notNullNotEmpty(fields)) {
            List<ReturnField> filters = new ArrayList<>();
            for (String field : fields.replaceAll("\\s", "").split(",")) {
                filters.add(returnFieldConfig.getReturnFieldByName(field));
            }
            filters.addAll(returnFieldConfig.getRequiredReturnFields());
            return filters;
        } else {
            return Collections.emptyList();
        }
    }

    private void writeJsonArray(
            JsonGenerator generator, String arrayFieldName, Collection<?> values)
            throws IOException {
        generator.writeFieldName(arrayFieldName);
        generator.writeStartArray();
        for (Object matchedField : values) {
            writeElement(generator, matchedField);
        }
        generator.writeEndArray();
    }

    private static void writeJsonObject(
            JsonGenerator generator, String fieldName, Object fieldValue) throws IOException {
        generator.writeFieldName(fieldName);
        generator.writeObject(fieldValue);
    }

    private void writeElement(JsonGenerator generator, Object facet) {
        try {
            generator.writeObject(facet);
        } catch (IOException e) {
            throw new StopStreamException("Failed to write JSON object", e);
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
                        new SimpleSquigglyContextProvider(SQUIGGLY_PARSER, fieldsPath));

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
            Matcher match = FILTER_PATTERN.matcher(currentFilter);
            Matcher newMatch = FILTER_PATTERN.matcher(filter);
            if (match.find() && newMatch.find()) {
                String newFilter = "[?(" + match.group(1) + " || " + newMatch.group(1) + ")]";
                filters.put(path, newFilter);
            }
        } else {
            filters.put(path, filter);
        }
    }
}
