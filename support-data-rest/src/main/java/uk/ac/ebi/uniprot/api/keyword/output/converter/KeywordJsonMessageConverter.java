package uk.ac.ebi.uniprot.api.keyword.output.converter;

import uk.ac.ebi.uniprot.api.keyword.output.KeywordEntryFilter;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.json.parser.keyword.KeywordJsonConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lgonzales
 */
public class KeywordJsonMessageConverter extends AbstractJsonMessageConverter<KeywordEntry> {

    public KeywordJsonMessageConverter() {
        super(KeywordJsonConfig.getInstance().getSimpleObjectMapper(), KeywordEntry.class);
    }

    @Override
    protected KeywordEntry filterEntryContent(KeywordEntry entity) {
        Map<String, List<String>> filters = getThreadLocalFilterMap();
        if (filters != null && !filters.isEmpty()) {
            entity = KeywordEntryFilter.filterEntry(entity, new ArrayList<>(filters.keySet()));
        }
        return entity;
    }

}