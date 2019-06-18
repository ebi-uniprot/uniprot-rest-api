package uk.ac.ebi.uniprot.api.keyword.output.converter;

import uk.ac.ebi.uniprot.api.keyword.output.KeywordEntryFilter;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.parser.tsv.keyword.KeywordEntryMap;
import uk.ac.ebi.uniprot.search.field.KeywordField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeywordTsvMessageConverter extends AbstractTsvMessagerConverter<KeywordEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public KeywordTsvMessageConverter() {
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
