package org.uniprot.api.uniprotkb.output.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXslMessegerConverter;
import org.uniprot.api.rest.output.converter.UniProtkbEntryFilters;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.core.parser.tsv.uniprot.EntryMap;
import org.uniprot.core.uniprotkb.UniProtkbEntry;
import org.uniprot.store.search.domain.Field;
import org.uniprot.store.search.domain.impl.UniProtResultFields;

public class UniProtKBXslMessageConverter extends AbstractXslMessegerConverter<UniProtkbEntry> {
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public UniProtKBXslMessageConverter() {
        super(UniProtkbEntry.class);
    }

    @Override
    protected void initBefore(MessageConverterContext<UniProtkbEntry> context) {
        tlFilters.set(FieldsParser.parseForFilters(context.getFields()));
        tlFields.set(FieldsParser.parse(context.getFields()));
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    @Override
    protected List<String> entry2TsvStrings(UniProtkbEntry entity) {
        Map<String, List<String>> filterParams = tlFilters.get();

        List<String> fields = tlFields.get();
        if ((filterParams != null) && !filterParams.isEmpty())
            UniProtkbEntryFilters.filterEntry(entity, filterParams);
        EntryMap dlEntry = new EntryMap(entity, fields);
        return dlEntry.getData();
    }

    private String getFieldDisplayName(String field) {
        Optional<Field> opField = UniProtResultFields.INSTANCE.getField(field);
        if (opField.isPresent()) return opField.get().getLabel();
        else return field;
    }
}
