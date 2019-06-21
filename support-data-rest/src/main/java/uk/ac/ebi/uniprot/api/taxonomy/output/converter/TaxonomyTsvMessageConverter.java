package uk.ac.ebi.uniprot.api.taxonomy.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.TaxonomyEntryFilter;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.parser.tsv.taxonomy.TaxonomyEntryMap;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaxonomyTsvMessageConverter extends AbstractTsvMessagerConverter<TaxonomyEntry> {

    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public TaxonomyTsvMessageConverter() {
        super(TaxonomyEntry.class);
    }

    @Override
    protected List<String> entry2TsvStrings(TaxonomyEntry entity) {
        Map<String, String> mappedField = new TaxonomyEntryMap(entity).attributeValues();
        return getData(mappedField);
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private String getFieldDisplayName(String fieldName) {
        return TaxonomyField.ResultFields.valueOf(fieldName).getLabel();
    }

    @Override
    protected void initBefore(MessageConverterContext<TaxonomyEntry> context) {
        tlFields.set(TaxonomyEntryFilter.parse(context.getFields()));
    }

    public List<String> getData(Map<String, String> mappedField) {
        List<String> fields = tlFields.get();

        return fields.stream()
                .map(field -> mappedField.getOrDefault(field,""))
                .collect(Collectors.toList());
    }
}
