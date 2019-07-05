package uk.ac.ebi.uniprot.api.literature.output.converter;

import uk.ac.ebi.uniprot.api.literature.output.LiteratureEntryFilter;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.parser.tsv.literature.LiteratureEntryMap;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureTsvMessageConverter extends AbstractTsvMessagerConverter<LiteratureEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public LiteratureTsvMessageConverter() {
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
