package org.uniprot.api.keyword.output.converter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.uniprot.api.keyword.output.KeywordEntryFilter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXslMessegerConverter;
import org.uniprot.core.parser.tsv.keyword.KeywordEntryMap;
import org.uniprot.cv.keyword.KeywordEntry;
import org.uniprot.store.search.field.KeywordField;

public class KeywordXlsMessageConverter extends AbstractXslMessegerConverter<KeywordEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public KeywordXlsMessageConverter() {
        super(KeywordEntry.class);
    }

    @Override
    protected List<String> entry2TsvStrings(KeywordEntry entity) {
        Map<String, String> mappedField = new KeywordEntryMap(entity).attributeValues();
        return getData(mappedField);
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private String getFieldDisplayName(String fieldName) {
        return KeywordField.ResultFields.valueOf(fieldName).getLabel();
    }

    @Override
    protected void initBefore(MessageConverterContext<KeywordEntry> context) {
        tlFields.set(KeywordEntryFilter.parse(context.getFields()));
    }

    public List<String> getData(Map<String, String> mappedField) {
        List<String> fields = tlFields.get();

        return fields.stream()
                .map(field -> mappedField.getOrDefault(field, ""))
                .collect(Collectors.toList());
    }
}
