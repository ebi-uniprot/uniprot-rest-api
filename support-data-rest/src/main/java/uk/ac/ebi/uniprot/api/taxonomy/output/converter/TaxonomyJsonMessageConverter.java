package uk.ac.ebi.uniprot.api.taxonomy.output.converter;

import com.google.common.base.Strings;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.TaxonomyEntryFilter;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;

import java.util.*;

public class TaxonomyJsonMessageConverter extends AbstractJsonMessageConverter<TaxonomyEntry> {

    private static final String COMMA = "\\s*,\\s*";

    public TaxonomyJsonMessageConverter() {
        super(TaxonomyJsonConfig.getInstance().getSimpleObjectMapper());
    }

    @Override
    protected TaxonomyEntry filterEntryContent(TaxonomyEntry entity) {
        Map<String, List<String>> filters = getThreadLocalFilterMap();
        if (filters != null && !filters.isEmpty()) {
            entity = TaxonomyEntryFilter.filterEntry(entity, new ArrayList<>(filters.keySet()));
        }
        return entity;
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        if (Strings.isNullOrEmpty(fields)) {
            return Collections.emptyMap();
        }else {
            Map<String, List<String>> filters = new HashMap<>();
            for(String field: fields.split(COMMA)){
                filters.put(field, Collections.emptyList());
            }
            return filters;
        }
    }
}