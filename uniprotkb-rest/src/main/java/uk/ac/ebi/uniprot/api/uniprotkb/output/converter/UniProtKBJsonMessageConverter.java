package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.service.filters.EntryFilters;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.json.parser.uniprot.UniprotJsonConfig;

import java.util.List;
import java.util.Map;

public class UniProtKBJsonMessageConverter extends AbstractJsonMessageConverter<UniProtEntry> {

    public UniProtKBJsonMessageConverter() {
        super(UniprotJsonConfig.getInstance().getSimpleObjectMapper(), UniProtEntry.class);
    }

    @Override
    protected UniProtEntry filterEntryContent(UniProtEntry entity) {
        Map<String, List<String>> filters = getThreadLocalFilterMap();
        if (filters != null && !filters.isEmpty()) {
        	entity = EntryFilters.filterEntry(entity, filters);
        }
        return entity;
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return FieldsParser.parseForFilters(fields);
    }
}
