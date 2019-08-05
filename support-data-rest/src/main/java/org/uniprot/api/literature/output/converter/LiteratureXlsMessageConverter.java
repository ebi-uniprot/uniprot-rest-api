package org.uniprot.api.literature.output.converter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.uniprot.api.literature.output.LiteratureEntryFilter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXslMessegerConverter;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.parser.tsv.literature.LiteratureEntryMap;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureXlsMessageConverter extends AbstractXslMessegerConverter<LiteratureEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public LiteratureXlsMessageConverter() {
        super(LiteratureEntry.class);
    }

    @Override
    protected List<String> entry2TsvStrings(LiteratureEntry entity) {
        Map<String, String> mappedField = new LiteratureEntryMap(entity).attributeValues();
        return getData(mappedField);
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private String getFieldDisplayName(String fieldName) {
        return LiteratureField.ResultFields.valueOf(fieldName).getLabel();
    }

    @Override
    protected void initBefore(MessageConverterContext<LiteratureEntry> context) {
        tlFields.set(LiteratureEntryFilter.parse(context.getFields()));
    }

    public List<String> getData(Map<String, String> mappedField) {
        List<String> fields = tlFields.get();

        return fields.stream()
                .map(field -> mappedField.getOrDefault(field, ""))
                .collect(Collectors.toList());
    }
}
