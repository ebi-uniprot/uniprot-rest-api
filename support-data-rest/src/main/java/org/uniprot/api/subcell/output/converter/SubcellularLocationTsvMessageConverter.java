package org.uniprot.api.subcell.output.converter;

import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import org.uniprot.api.subcell.output.SubcellularLocationEntryFilter;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.parser.tsv.subcell.SubcellularLocationEntryMap;
import org.uniprot.store.search.field.SubcellularLocationField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
public class SubcellularLocationTsvMessageConverter extends AbstractTsvMessagerConverter<SubcellularLocationEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public SubcellularLocationTsvMessageConverter() {
        super(SubcellularLocationEntry.class);
    }

    @Override
    protected List<String> entry2TsvStrings(SubcellularLocationEntry entity) {
        Map<String, String> mappedField = new SubcellularLocationEntryMap(entity).attributeValues();
        return getData(mappedField);
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private String getFieldDisplayName(String fieldName) {
        return SubcellularLocationField.ResultFields.valueOf(fieldName).getLabel();
    }

    @Override
    protected void initBefore(MessageConverterContext<SubcellularLocationEntry> context) {
        tlFields.set(SubcellularLocationEntryFilter.parse(context.getFields()));
    }

    public List<String> getData(Map<String, String> mappedField) {
        List<String> fields = tlFields.get();

        return fields.stream()
                .map(field -> mappedField.getOrDefault(field, ""))
                .collect(Collectors.toList());
    }
}
