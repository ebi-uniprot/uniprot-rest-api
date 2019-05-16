package uk.ac.ebi.uniprot.api.taxonomy.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonomyJsonMessageConverter extends AbstractJsonMessageConverter<TaxonomyEntry> {

    public TaxonomyJsonMessageConverter() {
        super(TaxonomyJsonConfig.getInstance().getSimpleObjectMapper());
    }

    @Override
    protected TaxonomyEntry filterEntryContent(TaxonomyEntry entity) {
        return entity; //TODO: implement the filter logic for taxonomy...
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return new HashMap<>(); //TODO: implement the filter map creation for taxonomy...
    }
}