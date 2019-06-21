package uk.ac.ebi.uniprot.api.taxonomy.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.TaxonomyEntryFilter;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaxonomyJsonMessageConverter extends AbstractJsonMessageConverter<TaxonomyEntry> {

    private static final String COMMA = "\\s*,\\s*";

    public TaxonomyJsonMessageConverter() {
        super(TaxonomyJsonConfig.getInstance().getSimpleObjectMapper(), TaxonomyEntry.class);
    }

    @Override
    protected TaxonomyEntry filterEntryContent(TaxonomyEntry entity) {
        Map<String, List<String>> filters = getThreadLocalFilterMap();
        if (filters != null && !filters.isEmpty()) {
            entity = TaxonomyEntryFilter.filterEntry(entity, new ArrayList<>(filters.keySet()));
        }
        return entity;
    }
}